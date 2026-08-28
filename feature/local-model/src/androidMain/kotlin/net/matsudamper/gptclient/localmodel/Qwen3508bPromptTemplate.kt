package net.matsudamper.gptclient.localmodel

internal object Qwen3508bPromptTemplate {
    val overwritePromptTemplate: String =
        """
{%- for message in messages -%}{%- if message.role == 'system' -%}<|im_start|>system
{{ message.content }}<|im_end|>
{%- elif message.role == 'user' -%}<|im_start|>user
{%- if message.content is string -%}{{ message.content }}{%- else -%}{%- for item in message.content -%}{%- if item.type == 'text' -%}{{ item.text }}{%- elif item.type == 'image' -%}<start_of_image>{%- endif -%}{%- endfor -%}{%- endif -%}<|im_end|>
{%- elif message.role == 'assistant' -%}<|im_start|>assistant
{%- if message.content is string -%}{{ message.content }}{%- else -%}{%- for item in message.content -%}{%- if item.type == 'text' -%}{{ item.text }}{%- endif -%}{%- endfor -%}{%- endif -%}<|im_end|>
{%- endif -%}{%- endfor -%}{%- if add_generation_prompt -%}<|im_start|>assistant
<think>

</think>

{%- endif -%}
        """.trimIndent()
}
