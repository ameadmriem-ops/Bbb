package com.example.util

import com.example.model.ToolCategory
import com.example.model.ToolItem

object AppToolsList {

    val tools: List<ToolItem> = listOf(
        ToolItem(
            id = "ai_writer",
            titleAr = "الكاتب الذكي وصياغة المحتوى",
            titleEn = "AI Smart Writer",
            titleFr = "Rédacteur Intelligent",
            descAr = "كتابة مقالات، منشورات سوشيال ميديا، رسائل بريد رسمية، وإعادة صياغة النصوص باحترافية.",
            descEn = "Write essays, social posts, professional emails, and rewrite content.",
            descFr = "Rédigez des articles, des e-mails professionnels et reformulez du contenu.",
            iconName = "Edit",
            category = ToolCategory.WRITING,
            defaultPrompt = "أنت كاتب محترف وخبير في صياغة المحتوى. قم بكتابة أو إعادة صياغة النص التالي بأسلوب جذاب ومتقن:\n\n",
            inputPlaceholderAr = "اكتب الموضوع، الفكرة، أو النص المراد صياغته...",
            inputPlaceholderEn = "Enter the topic or text you want to rewrite...",
            inputPlaceholderFr = "Entrez le sujet ou le texte à reformuler..."
        ),
        ToolItem(
            id = "code_assistant",
            titleAr = "مساعد المبرمجين والأكواد",
            titleEn = "Code Assistant & Debugger",
            titleFr = "Assistant Code & Debug",
            descAr = "كتابة أكواد بجميع اللغات (Kotlin, Python, JS, C++), حل المشاكل البرمجية وشرح الخوارزميات.",
            descEn = "Generate clean code, debug issues, and explain complex algorithms.",
            descFr = "Générez du code propre, corrigez les bugs et expliquez les algorithmes.",
            iconName = "Code",
            category = ToolCategory.CODING,
            defaultPrompt = "أنت كبير مهندسي البرمجيات. اكتب كوداً نظيفاً وموثقاً مع معالجة الأخطاء والشرح:\n\n",
            inputPlaceholderAr = "صف البرنامج المطلوب أو الصق كودك لاكتشاف الأخطاء...",
            inputPlaceholderEn = "Describe the software needed or paste code to debug...",
            inputPlaceholderFr = "Décrivez le programme ou collez du code pour le déboguer..."
        ),
        ToolItem(
            id = "translator",
            titleAr = "المترجم الفوري الذكي",
            titleEn = "Smart Multilingual Translator",
            titleFr = "Traducteur Intelligent",
            descAr = "ترجمة احترافية تحافظ على المعنى الدقيق والسياق الثقافي بين العربية، الإنجليزية، الفرنسية، وغيرها.",
            descEn = "Context-aware translation preserving nuances across languages.",
            descFr = "Traduction contextuelle préservant les nuances.",
            iconName = "Translate",
            category = ToolCategory.TRANSLATION,
            defaultPrompt = "أنت مترجم فوري محترف. ترجم النص التالي ترجمة دقيقة واحترافية مع مراعاة السياق:\n\n",
            inputPlaceholderAr = "أدخل النص المراد ترجمته وحدد اللغة المستهدفة...",
            inputPlaceholderEn = "Enter text to translate and target language...",
            inputPlaceholderFr = "Entrez le texte à traduire et la langue cible..."
        ),
        ToolItem(
            id = "summarizer",
            titleAr = "الملخص السريع والمكثف",
            titleEn = "Executive Summarizer",
            titleFr = "Résumeur Express",
            descAr = "تحويل المقالات الطويلة والأبحاث والتقارير إلى نقاط مكثفة وأفكار جوهرية سريعة القراءة.",
            descEn = "Condense long articles and reports into key actionable bullet points.",
            descFr = "Condensez de longs rapports en points clés exploitables.",
            iconName = "Summarize",
            category = ToolCategory.ANALYSIS,
            defaultPrompt = "لخص النص التالي في نقاط موجزة وواضحة جداً مع استخراج الفكرة الجوهرية والنتائج الرئيسية:\n\n",
            inputPlaceholderAr = "الصق النص أو المقال الطويل هنا لتلخيصه...",
            inputPlaceholderEn = "Paste lengthy article or report here to summarize...",
            inputPlaceholderFr = "Collez un long article ou rapport ici à résumer..."
        ),
        ToolItem(
            id = "pdf_analyzer",
            titleAr = "محلل المستندات والملفات",
            titleEn = "Document & File Analyzer",
            titleFr = "Analyseur de Documents",
            descAr = "استخراج البيانات من ملفات PDF والملفات النصية، الإجابة على الأسئلة وتلخيص المستندات الضخمة.",
            descEn = "Extract data, ask questions, and comprehend documents & files.",
            descFr = "Extrayez des données et analysez des documents complexes.",
            iconName = "Description",
            category = ToolCategory.ANALYSIS,
            defaultPrompt = "حلل المستند المرفق واستخرج أهم الحقائق والأرقام والاستنتاجات:\n\n",
            inputPlaceholderAr = "اكتب استفساراتك حول الملف بعد إرفاقه أو الصق نصه...",
            inputPlaceholderEn = "Ask questions regarding the file or paste content...",
            inputPlaceholderFr = "Posez des questions sur le fichier ou collez le contenu..."
        ),
        ToolItem(
            id = "voice_assistant",
            titleAr = "المساعد الصوتي التفاعلي",
            titleEn = "Interactive Voice Assistant",
            titleFr = "Assistant Vocal Interactif",
            descAr = "محادثة صوتية ذكية ثنائية الاتجاه، استمع للإجابات بأصوات طبيعية وتحدث بصوتك مباشرة.",
            descEn = "Two-way conversational voice interaction with natural speech synthesis.",
            descFr = "Interaction vocale bidirectionnelle avec synthèse vocale naturelle.",
            iconName = "RecordVoiceOver",
            category = ToolCategory.PRODUCTIVITY,
            defaultPrompt = "أنت مساعد صوتي ذكي ودود. أجب بإيجاز وبشكل ملائم للقراءة الصوتية:\n\n",
            inputPlaceholderAr = "تحدث بصوتك باستخدام الميكروفون أو اكتب سؤالك الصوتي...",
            inputPlaceholderEn = "Speak using microphone or type query...",
            inputPlaceholderFr = "Parlez avec le microphone ou écrivez..."
        )
    )
}
