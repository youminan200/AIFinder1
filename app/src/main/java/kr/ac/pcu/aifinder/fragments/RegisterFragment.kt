package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val displayName = binding.etDisplayName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (username.isEmpty() || displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "모든 정보를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val newUser = User(
                    id = "user_${System.currentTimeMillis()}",
                    username = username,
                    passwordHash = password,
                    email = email,
                    displayName = displayName
                )
                val response = itemStorage.registerUserRemote(newUser)
                if (response.success) {
                    val registeredUser = response.user ?: newUser
                    itemStorage.saveOrUpdateUser(registeredUser)
                    itemStorage.setCurrentUser(registeredUser.id)
                    itemStorage.setAutoLoginEnabled(binding.cbAutoLogin.isChecked)
                    (activity as? MainActivity)?.onLoginSuccess()
                } else {
                    // Check if already registered
                    val isAlreadyRegistered = response.message?.contains("already") == true ||
                            response.message?.contains("in use") == true ||
                            response.message?.contains("exists") == true
                    
                    if (isAlreadyRegistered) {
                        val loginResponse = itemStorage.authenticateRemote(email, password)
                        if (loginResponse.success) {
                            val loggedInUser = loginResponse.user ?: newUser
                            itemStorage.saveOrUpdateUser(loggedInUser)
                            itemStorage.setCurrentUser(loggedInUser.id)
                            itemStorage.setAutoLoginEnabled(binding.cbAutoLogin.isChecked)
                            (activity as? MainActivity)?.onLoginSuccess()
                        } else {
                            Toast.makeText(context, "이미 가입된 계정이나 비밀번호가 틀립니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "가입 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.showLoginFragment()
        }

        binding.tvBackToLogin.setOnClickListener {
            (activity as? MainActivity)?.showLoginFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
