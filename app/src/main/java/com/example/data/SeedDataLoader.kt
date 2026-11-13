package com.example.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SeedDataLoader {

    private const val TAG = "SeedDataLoader"

    suspend fun seedMarketplaceDataIfEmpty(db: FirebaseFirestore) {
        try {
            val coursesSnapshot = db.collection("courses").limit(1).get().await()
            if (!coursesSnapshot.isEmpty) {
                Log.d(TAG, "Marketplace data already seeded.")
                return
            }

            Log.d(TAG, "Seeding marketplace categories, courses, lessons and announcements...")

            // 1. Categories
            val categories = listOf(
                mapOf("id" to "cat_android", "name" to "Android Development", "nameBn" to "অ্যান্ড্রয়েড ডেভেলপমেন্ট", "icon" to "android", "courseCount" to 3L),
                mapOf("id" to "cat_web", "name" to "Full-Stack Web", "nameBn" to "ফুল-স্ট্যাক ওয়েব", "icon" to "web", "courseCount" to 2L),
                mapOf("id" to "cat_ai", "name" to "AI & Machine Learning", "nameBn" to "এআই ও মেশিন লার্নিং", "icon" to "psychology", "courseCount" to 2L),
                mapOf("id" to "cat_cloud", "name" to "Cloud & DevOps", "nameBn" to "ক্লাউড ও ডেভঅপস", "icon" to "cloud", "courseCount" to 2L),
                mapOf("id" to "cat_cyber", "name" to "Cybersecurity", "nameBn" to "সাইবার নিরাপত্তা", "icon" to "security", "courseCount" to 1L),
                mapOf("id" to "cat_design", "name" to "UI/UX & Product Design", "nameBn" to "ইউআই/ইউএক্স ডিজাইন", "icon" to "palette", "courseCount" to 1L)
            )

            for (cat in categories) {
                val catId = cat["id"] as String
                db.collection("categories").document(catId).set(cat).await()
            }

            // 2. Courses
            val courses = listOf(
                mapOf(
                    "id" to "course_kotlin_mastery",
                    "title" to "Modern Android with Kotlin & Jetpack Compose",
                    "titleBn" to "আধুনিক অ্যান্ড্রয়েড: কোটলিন ও জেটপ্যাক কম্পোজ",
                    "description" to "Master native Android app architecture, Compose M3, Coroutines, Flow, Room, and production Firebase integrations.",
                    "descriptionBn" to "নেটিভ অ্যান্ড্রয়েড আর্কিটেকচার, জেটপ্যাক কম্পোজ, কোরুটিন, রুম ডেটাবেস এবং ফায়ারবেস ইন্টিগ্রেশন শিখুন।",
                    "instructor" to "Dr. Arindam Roy",
                    "category" to "cat_android",
                    "price" to 1499.0,
                    "isFree" to false,
                    "duration" to "38 hours",
                    "level" to "Intermediate",
                    "rating" to 4.9,
                    "studentsCount" to 1240L,
                    "lessonsCount" to 4L,
                    "published" to true,
                    "featured" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "course_compose_starter",
                    "title" to "Jetpack Compose Zero to Hero (Free)",
                    "titleBn" to "জেটপ্যাক কম্পোজ শূন্য থেকে হিরো (ফ্রি)",
                    "description" to "A comprehensive beginner-friendly foundation course covering Compose state, modifiers, layouts, and Material 3 design.",
                    "descriptionBn" to "কম্পোজ স্টেট, মডিফায়ার, লেআউট এবং মেটেরিয়াল ৩ ডিজাইনের একটি সম্পূর্ণ শিক্ষানবিস কোর্স।",
                    "instructor" to "Priya Sharma",
                    "category" to "cat_android",
                    "price" to 0.0,
                    "isFree" to true,
                    "duration" to "12 hours",
                    "level" to "Beginner",
                    "rating" to 4.8,
                    "studentsCount" to 3820L,
                    "lessonsCount" to 3L,
                    "published" to true,
                    "featured" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "course_ai_llm_engineer",
                    "title" to "Generative AI & LLM Systems Engineering",
                    "titleBn" to "জেনারেটিভ এআই ও এলএলএম সিস্টেম ইঞ্জিনিয়ারিং",
                    "description" to "Build enterprise AI agents, RAG pipelines, fine-tuned models, and multimodal tools with Python, LangChain, and Gemini API.",
                    "descriptionBn" to "এন্টারপ্রাইজ এআই এজেন্ট, আরএজি পাইপলাইন এবং মাল্টিমোডাল টুলস তৈরি করুন পাইথন এবং জেমিনাই এপিআই দিয়ে।",
                    "instructor" to "Sourav Mukherjee",
                    "category" to "cat_ai",
                    "price" to 2199.0,
                    "isFree" to false,
                    "duration" to "45 hours",
                    "level" to "Advanced",
                    "rating" to 5.0,
                    "studentsCount" to 950L,
                    "lessonsCount" to 3L,
                    "published" to true,
                    "featured" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "course_fullstack_nextjs",
                    "title" to "Full-Stack Web with Next.js 15 & Node.js",
                    "titleBn" to "ফুল-স্ট্যাক ওয়েব: নেক্সট.জেএস ১৫ ও নোড.জেএস",
                    "description" to "Build production-ready web apps with Next.js App Router, Tailwind CSS, Server Actions, PostgreSQL, and Redis caching.",
                    "descriptionBn" to "নেক্সট.জেএস অ্যাপ রাউটার, টেলউইন্ড সিএসএস, পোস্টগ্রেসকিউএল ও ক্যাশিং দিয়ে পূর্ণাঙ্গ ওয়েব অ্যাপ তৈরি করুন।",
                    "instructor" to "Tanvir Ahmed",
                    "category" to "cat_web",
                    "price" to 1299.0,
                    "isFree" to false,
                    "duration" to "32 hours",
                    "level" to "Intermediate",
                    "rating" to 4.8,
                    "studentsCount" to 1540L,
                    "lessonsCount" to 3L,
                    "published" to true,
                    "featured" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "course_git_linux_free",
                    "title" to "Git, GitHub & Linux CLI Essentials (Free)",
                    "titleBn" to "গিট, গিটহাব ও লিনাক্স কমান্ড অপরিহার্য (ফ্রি)",
                    "description" to "Master everyday terminal workflows, version control branching, pull requests, SSH keys, and remote repository management.",
                    "descriptionBn" to "টার্মিনাল ওয়ার্কফ্লো, গিট ব্রাঞ্চিং, পুল রিকোয়েস্ট এবং রিমোট রিপোজিটরি ব্যবস্থাপনা আয়ত্ত করুন।",
                    "instructor" to "Niloy Chowdhury",
                    "category" to "cat_cloud",
                    "price" to 0.0,
                    "isFree" to true,
                    "duration" to "8 hours",
                    "level" to "Beginner",
                    "rating" to 4.9,
                    "studentsCount" to 5120L,
                    "lessonsCount" to 3L,
                    "published" to true,
                    "featured" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "course_cloud_devops_k8s",
                    "title" to "Docker, Kubernetes & AWS Cloud DevOps",
                    "titleBn" to "ডকার, কুবারনেটিস ও এডব্লিউএস ক্লাউড ডেভঅপস",
                    "description" to "Production infrastructure as code, CI/CD with GitHub Actions, Helm charts, container orchestration, and monitoring.",
                    "descriptionBn" to "ইনফ্রাস্ট্রাকচার এজ কোড, সিআই/সিডি অটোমেশন, কন্টেইনার অর্কেস্ট্রেশন এবং ক্লাউড আর্কিটেকচার শিখুন।",
                    "instructor" to "Rahul Sen",
                    "category" to "cat_cloud",
                    "price" to 1799.0,
                    "isFree" to false,
                    "duration" to "40 hours",
                    "level" to "Advanced",
                    "rating" to 4.9,
                    "studentsCount" to 820L,
                    "lessonsCount" to 3L,
                    "published" to true,
                    "featured" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            for (course in courses) {
                val courseId = course["id"] as String
                db.collection("courses").document(courseId).set(course).await()
            }

            // 3. Lessons
            val lessons = listOf(
                // For course_kotlin_mastery
                mapOf(
                    "id" to "les_km_1",
                    "courseId" to "course_kotlin_mastery",
                    "title" to "Lesson 1: Declarative UI Foundations & State in Compose",
                    "titleBn" to "পাঠ ১: ডিক্লারেটিভ ইউআই ভিত্তি ও স্টেট ম্যানেজমেন্ট",
                    "duration" to "42 min",
                    "order" to 1L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to true
                ),
                mapOf(
                    "id" to "les_km_2",
                    "courseId" to "course_kotlin_mastery",
                    "title" to "Lesson 2: Clean Architecture with ViewModel & Kotlin Flow",
                    "titleBn" to "পাঠ ২: ভিউমডেল ও কোটলিন ফ্লো সহ ক্লিন আর্কিটেকচার",
                    "duration" to "55 min",
                    "order" to 2L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to false
                ),
                mapOf(
                    "id" to "les_km_3",
                    "courseId" to "course_kotlin_mastery",
                    "title" to "Lesson 3: Firebase Firestore & Realtime Snapshot Synchronization",
                    "titleBn" to "পাঠ ৩: ফায়ারবেস ফায়ারস্টোর ও লাইভ স্ন্যাপশট সিঙ্ক",
                    "duration" to "68 min",
                    "order" to 3L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to false
                ),
                mapOf(
                    "id" to "les_km_4",
                    "courseId" to "course_kotlin_mastery",
                    "title" to "Lesson 4: In-App Payments, Play Store Security & Production CI/CD",
                    "titleBn" to "পাঠ ৪: ইন-অ্যাপ পেমেন্ট, সিকিউরিটি ও রিলিজ সিআই/সিডি",
                    "duration" to "50 min",
                    "order" to 4L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to false
                ),

                // For course_compose_starter
                mapOf(
                    "id" to "les_cs_1",
                    "courseId" to "course_compose_starter",
                    "title" to "Lesson 1: Introduction to Jetpack Compose & M3 Setup",
                    "titleBn" to "পাঠ ১: জেটপ্যাক কম্পোজ পরিচিতি ও মেটেরিয়াল ৩ সেটআপ",
                    "duration" to "30 min",
                    "order" to 1L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to true
                ),
                mapOf(
                    "id" to "les_cs_2",
                    "courseId" to "course_compose_starter",
                    "title" to "Lesson 2: Modifiers, Lazy Lists & Responsive Layouts",
                    "titleBn" to "পাঠ ২: মডিফায়ার, লেজি লিস্ট ও রেসপনসিভ লেআউট",
                    "duration" to "45 min",
                    "order" to 2L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to true
                ),
                mapOf(
                    "id" to "les_cs_3",
                    "courseId" to "course_compose_starter",
                    "title" to "Lesson 3: Building a Complete Store Dashboard",
                    "titleBn" to "পাঠ ৩: একটি পূর্ণাঙ্গ স্টোর ড্যাশবোর্ড তৈরি",
                    "duration" to "60 min",
                    "order" to 3L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to true
                ),

                // For course_ai_llm_engineer
                mapOf(
                    "id" to "les_ai_1",
                    "courseId" to "course_ai_llm_engineer",
                    "title" to "Lesson 1: LLM Prompt Architecture & Function Calling",
                    "titleBn" to "পাঠ ১: এলএলএম প্রম্পট আর্কিটেকচার ও ফাংশন কলিং",
                    "duration" to "50 min",
                    "order" to 1L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to true
                ),
                mapOf(
                    "id" to "les_ai_2",
                    "courseId" to "course_ai_llm_engineer",
                    "title" to "Lesson 2: Vector Embeddings & Production RAG Pipelines",
                    "titleBn" to "পাঠ ২: ভেক্টর এমবেডিংস ও প্রোডাকশন আরএজি পাইপলাইন",
                    "duration" to "75 min",
                    "order" to 2L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to false
                ),
                mapOf(
                    "id" to "les_ai_3",
                    "courseId" to "course_ai_llm_engineer",
                    "title" to "Lesson 3: Autonomous Multi-Agent Workflows",
                    "titleBn" to "পাঠ ৩: অটোনোমাস মাল্টি-এজেন্ট ওয়ার্কফ্লো",
                    "duration" to "65 min",
                    "order" to 3L,
                    "videoUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "pdfUrl" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "isPreview" to false
                )
            )

            for (les in lessons) {
                val lesId = les["id"] as String
                db.collection("lessons").document(lesId).set(les).await()
            }

            // 4. Announcements
            val announcements = listOf(
                mapOf(
                    "id" to "ann_welcome",
                    "title" to "Welcome to Zentrix Store Premium Academy!",
                    "titleBn" to "জেনট্রিক্স স্টোর প্রিমিয়াম একাডেমিতে স্বাগতম!",
                    "message" to "Explore industry-leading courses, live verified certificates, interactive video lessons, and curated downloadable resources.",
                    "messageBn" to "শিল্পমানের কোর্স, লাইভ ভেরিফাইড সার্টিফিকেট, ইন্টারঅ্যাক্টিভ ভিডিও এবং ডাউনলোডযোগ্য রিসোর্স অন্বেষণ করুন।",
                    "priority" to "HIGH",
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "id" to "ann_bengali_support",
                    "title" to "Bengali Language Support Is Now Live!",
                    "titleBn" to "বাংলা ভাষা সমর্থন এখন সক্রিয়!",
                    "message" to "You can switch between English and Bengali anytime from your Profile screen to view full translated course catalogs.",
                    "messageBn" to "কোর্সের সমস্ত বিবরণ বাংলায় দেখতে আপনার প্রোফাইল স্ক্রিন থেকে যেকোনো সময় ইংরেজি ও বাংলার মধ্যে ভাষা পরিবর্তন করতে পারেন।",
                    "priority" to "NORMAL",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            for (ann in announcements) {
                val annId = ann["id"] as String
                db.collection("announcements").document(annId).set(ann).await()
            }

            Log.d(TAG, "Marketplace seed completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed marketplace data", e)
        }
    }
}
