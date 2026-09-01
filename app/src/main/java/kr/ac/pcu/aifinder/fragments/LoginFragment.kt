package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val response = itemStorage.authenticateRemote(username, password)
                if (response.success && response.user != null) {
                    itemStorage.saveOrUpdateUser(response.user!!)
                    itemStorage.setCurrentUser(response.user!!.id)
                    itemStorage.setAutoLoginEnabled(binding.cbAutoLogin.isChecked)
                    (activity as? MainActivity)?.onLoginSuccess()
                } else {
                    Toast.makeText(context, "로그인 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvRegister.setOnClickListener {
            (activity as? MainActivity)?.showRegisterFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
