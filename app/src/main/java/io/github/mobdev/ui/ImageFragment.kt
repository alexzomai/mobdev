package io.github.mobdev.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import coil.load
import io.github.mobdev.databinding.FragmentImageBinding
import io.github.mobdev.network.ApiClient

class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private val imagePath: String
        get() = requireArguments().getString(ARG_PATH)!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.btnClose.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                topMargin = sys.top + (8 * resources.displayMetrics.density).toInt()
            }
            insets
        }

        binding.imageFullscreen.load("${ApiClient.BASE_IMAGE_URL}img/$imagePath")
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PATH = "path"

        fun newInstance(path: String) = ImageFragment().apply {
            arguments = Bundle().apply { putString(ARG_PATH, path) }
        }
    }
}
