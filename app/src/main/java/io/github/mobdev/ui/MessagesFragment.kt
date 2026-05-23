package io.github.mobdev.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.MainActivity
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentMessagesBinding
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val channel: String
        get() = requireArguments().getString(ARG_CHANNEL)!!

    private val viewModel: MessagesViewModel by activityViewModels {
        (requireActivity() as MainActivity).vmFactory
    }

    private lateinit var adapter: MessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = channel

        val isLandscape = requireActivity().findViewById<View?>(R.id.container_detail) != null
        if (!isLandscape) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationContentDescription(R.string.back)
            binding.toolbar.setNavigationOnClickListener {
                (activity as? MainActivity)?.goToChats()
            }
        }

        viewModel.setChannel(channel)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, sys.top, 0, maxOf(ime.bottom, sys.bottom))
            insets
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    binding.root.setPadding(0, binding.root.paddingTop, 0, maxOf(ime.bottom, sys.bottom))
                    return insets
                }
            },
        )

        adapter = MessagesAdapter { imagePath ->
            (activity as? MainActivity)?.openImage(imagePath)
        }

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.layoutManager = layoutManager
        binding.recyclerMessages.adapter = adapter

        binding.recyclerMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    viewModel.loadMore()
                }
            }
        })

        val showKeyboard = {
            if (!binding.editMessage.hasFocus()) binding.editMessage.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editMessage, InputMethodManager.SHOW_IMPLICIT)
            Unit
        }
        binding.editMessage.setOnClickListener { showKeyboard() }
        binding.editMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard()
        }

        // Установить начальное состояние сразу, не ждать первой эмиссии
        applyOnlineState(viewModel.isOnline.value)

        binding.btnSend.setOnClickListener {
            val text = binding.editMessage.text?.toString().orEmpty().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.editMessage.text?.clear()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { messages ->
                    val wasAtBottom = !binding.recyclerMessages.canScrollVertically(1)
                    adapter.submitList(messages) {
                        if (wasAtBottom) {
                            binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                    updateEmptyState(messages.isEmpty(), viewModel.isOnline.value, viewModel.isLoading.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnline.collect { online ->
                    applyOnlineState(online)
                    updateEmptyState(viewModel.messages.value.isEmpty(), online, viewModel.isLoading.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    updateEmptyState(viewModel.messages.value.isEmpty(), viewModel.isOnline.value, loading)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unauthorized.collect {
                    (activity as? MainActivity)?.goToLogin()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errors.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun applyOnlineState(online: Boolean) {
        binding.bannerOffline.visibility = if (online) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean, isOnline: Boolean, isLoading: Boolean) {
        if (!isEmpty || isLoading) {
            binding.emptyState.visibility = View.GONE
            return
        }
        binding.emptyState.visibility = View.VISIBLE
        if (isOnline) {
            binding.emptyIcon.text = "💬"
            binding.emptyText.text = getString(R.string.empty_channel_title)
            binding.emptySubtext.text = getString(R.string.empty_channel_sub)
        } else {
            binding.emptyIcon.text = "📡"
            binding.emptyText.text = getString(R.string.empty_no_network_title)
            binding.emptySubtext.text = getString(R.string.empty_no_network_sub)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CHANNEL = "channel"

        fun newInstance(channel: String) = MessagesFragment().apply {
            arguments = Bundle().apply { putString(ARG_CHANNEL, channel) }
        }
    }
}
