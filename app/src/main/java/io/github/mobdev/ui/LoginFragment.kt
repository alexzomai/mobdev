package io.github.mobdev.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.mobdev.MainActivity
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentLoginBinding
import io.github.mobdev.network.TokenStore
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LoginViewModel(TokenStore(requireContext())) as T
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.btnLogin.setOnClickListener {
            val login = binding.editLogin.text?.toString().orEmpty().trim()
            val password = binding.editPassword.text?.toString().orEmpty()
            if (login.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(login, password)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginViewModel.State.Loading -> {
                            binding.btnLogin.isEnabled = false
                        }
                        is LoginViewModel.State.Success -> {
                            (activity as? MainActivity)?.goToChats()
                        }
                        is LoginViewModel.State.Error -> {
                            binding.btnLogin.isEnabled = true
                            val message = if (state.message == "wrong_credentials") {
                                getString(R.string.error_wrong_credentials)
                            } else {
                                getString(R.string.error_network)
                            }
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.error_title)
                                .setMessage(message)
                                .setPositiveButton(R.string.ok, null)
                                .show()
                        }
                        is LoginViewModel.State.Idle -> {
                            binding.btnLogin.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
