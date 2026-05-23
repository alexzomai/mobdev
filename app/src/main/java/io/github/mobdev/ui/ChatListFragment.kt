package io.github.mobdev.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.mobdev.MainActivity
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentChatListBinding
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val appViewModel: AppViewModel by activityViewModels()

    private val viewModel: ChatListViewModel by activityViewModels {
        (requireActivity() as MainActivity).vmFactory
    }

    private lateinit var adapter: ChannelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }

        val isLandscape = requireActivity().findViewById<View?>(R.id.container_detail) != null

        adapter = ChannelAdapter(
            selectedChannel = if (isLandscape) appViewModel.selectedChannel.value else null,
            onClick = { channel ->
                appViewModel.selectChannel(channel)
                (activity as? MainActivity)?.openChannel(channel)
            }
        )
        binding.recyclerChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChats.adapter = adapter

        viewModel.ensureLoaded()

        binding.btnLogout.setOnClickListener {
            viewModel.logout {
                (activity as? MainActivity)?.goToLogin()
            }
        }

        binding.fabNewChat.setOnClickListener {
            showNewChatDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channels.collect { channels ->
                    adapter.submitList(channels)
                }
            }
        }

        if (isLandscape) {
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    appViewModel.selectedChannel.collect { selected ->
                        adapter.setSelected(selected)
                    }
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
    }

    private fun showNewChatDialog() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val layout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.new_chat_hint)
            setPadding(pad, pad, pad, 0)
        }
        val input = TextInputEditText(requireContext()).apply {
            setSingleLine()
        }
        layout.addView(input)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_chat)
            .setView(layout)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val raw = input.text?.toString().orEmpty().trim()
            if (raw.isEmpty()) {
                layout.error = getString(R.string.new_chat_hint)
                return@setOnClickListener
            }
            val channelName = if (raw.endsWith("@channel")) raw else "$raw@channel"
            dialog.dismiss()
            appViewModel.selectChannel(channelName)
            (activity as? MainActivity)?.openChannel(channelName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
