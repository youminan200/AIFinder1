package kr.ac.pcu.aifinder.fragments

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.pcu.aifinder.*
import kr.ac.pcu.aifinder.databinding.DialogAiAssistantBinding
import kr.ac.pcu.aifinder.databinding.ItemChatMessageBinding
import kotlinx.coroutines.launch

class AiAssistantDialogFragment : DialogFragment() {
    private var _binding: DialogAiAssistantBinding? = null
    private val binding get() = _binding!!
    private lateinit var itemStorage: ItemStorage
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemStorage = ItemStorage(PlatformStorage(requireContext()))

        setupChat()
        setupSuggestions()

        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }

        if (messages.isEmpty()) {
            messages.add(ChatMessage("안녕하세요! 보관 물품 찾기 AI 비서입니다. 🤖\n'가위 어디 있어?' 혹은 '거실에 있는 물건은?' 같은 질문을 남겨보세요!", false))
            chatAdapter.notifyItemInserted(0)
        }
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(context)
        binding.rvChat.adapter = chatAdapter
    }

    private fun setupSuggestions() {
        val suggestions = listOf("지갑 어딨어?", "최근 등록한 물건?", "모든 보관 목록?")
        suggestions.forEach { suggestion ->
            val button = com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = suggestion
                textSize = 11f
                setPadding(16, 0, 16, 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 8, 0)
                }
                setOnClickListener { sendMessage(suggestion) }
            }
            binding.layoutSuggestions.addView(button)
        }
    }

    private fun sendMessage(text: String) {
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
        binding.etInput.text?.clear()

        // Bot response
        lifecycleScope.launch {
            val items = itemStorage.getItems()
            val areas = itemStorage.getRoomAreas()
            val answer = askGeminiAssistant(requireContext(), items, areas, text)
            messages.add(ChatMessage(answer, false))
            chatAdapter.notifyItemInserted(messages.size - 1)
            binding.rvChat.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ChatAdapter(private val chatMessages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        inner class ChatViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChatViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = chatMessages[position]
            holder.binding.tvText.text = message.text
            
            val params = holder.binding.cardMessage.layoutParams as LinearLayout.LayoutParams
            if (message.isUser) {
                holder.binding.layoutRoot.gravity = Gravity.END
                holder.binding.cardMessage.setCardBackgroundColor(android.graphics.Color.parseColor("#4F46E5"))
                holder.binding.tvText.setTextColor(android.graphics.Color.WHITE)
                params.setMargins(48, 0, 0, 0)
            } else {
                holder.binding.layoutRoot.gravity = Gravity.START
                holder.binding.cardMessage.setCardBackgroundColor(android.graphics.Color.WHITE)
                holder.binding.tvText.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                params.setMargins(0, 0, 48, 0)
            }
            holder.binding.cardMessage.layoutParams = params
        }

        override fun getItemCount(): Int = chatMessages.size
    }

    data class ChatMessage(val text: String, val isUser: Boolean)
}
