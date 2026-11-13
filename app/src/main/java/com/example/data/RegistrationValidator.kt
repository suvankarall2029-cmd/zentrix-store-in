package com.example.data

import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale

data class CountryDialCode(
    val code: String,
    val countryName: String,
    val minDigits: Int,
    val maxDigits: Int
)

data class Step1Validation(
    val firstNameError: String? = null,
    val middleNameError: String? = null,
    val lastNameError: String? = null,
    val isValid: Boolean = false
)

data class Step2Validation(
    val emailError: String? = null,
    val phoneError: String? = null,
    val generalError: String? = null,
    val hasEmail: Boolean = false,
    val hasPhone: Boolean = false,
    val isValid: Boolean = false
)

data class PasswordEvaluation(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false,
    val passwordsMatch: Boolean = false,
    val strengthScore: Int = 0,
    val strengthLabel: String = "Too Weak",
    val errorMessage: String? = null,
    val isValid: Boolean = false
)

data class Step4Validation(
    val ageError: String? = null,
    val privacyNotice: String? = null,
    val purposeError: String? = null,
    val interestsError: String? = null,
    val resolvedAge: Int? = null,
    val ageGroup: String = "",
    val isValid: Boolean = false
)

object RegistrationConstants {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "suvankarall2029@gmail.com"
    const val MIN_ALLOWED_AGE = 13
    const val TEEN_AGE_THRESHOLD = 18

    val COUNTRY_CODES = listOf(
        CountryDialCode("+91", "India", 10, 10),
        CountryDialCode("+880", "Bangladesh", 10, 11),
        CountryDialCode("+1", "United States / Canada", 10, 10),
        CountryDialCode("+44", "United Kingdom", 10, 11),
        CountryDialCode("+971", "UAE", 9, 9),
        CountryDialCode("+65", "Singapore", 8, 8),
        CountryDialCode("+61", "Australia", 9, 9),
        CountryDialCode("+49", "Germany", 10, 12)
    )

    val PURPOSE_OPTIONS = listOf(
        "App Developer",
        "Software Developer",
        "Android App Developer",
        "Web Developer",
        "Code Seller",
        "Source Code Buyer",
        "App Buyer",
        "Website Buyer",
        "Course Learner",
        "PDF / Notes Buyer",
        "Digital Product Seller",
        "Student",
        "Freelancer",
        "Business Owner",
        "Other"
    )

    val INTEREST_OPTIONS = listOf(
        "Android Applications",
        "Web Development",
        "HTML / CSS / JavaScript",
        "Java / Kotlin",
        "Python",
        "PHP",
        "Flutter",
        "React",
        "Source Code",
        "Premium Templates",
        "PDF Notes",
        "Online Courses",
        "Digital Products",
        "Freelancing"
    )

    val AGE_GROUP_FILTERS = listOf("All", "13-17", "18-24", "25-34", "35-44", "45+")
}

object RegistrationValidator {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    private val NAME_REGEX = Regex("^[\\p{L}][\\p{L}\\s.'-]{1,39}$")
    private val SPECIAL_CHAR_REGEX = Regex("[^A-Za-z0-9\\s]")

    fun formatPersonName(raw: String): String {
        val collapsed = raw.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isBlank()) return ""
        return collapsed.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
            }
        }
    }

    fun buildFullName(firstName: String, middleName: String, lastName: String): String {
        return listOf(
            formatPersonName(firstName),
            formatPersonName(middleName),
            formatPersonName(lastName)
        ).filter { it.isNotBlank() }.joinToString(" ")
    }

    fun validateStep1(firstName: String, middleName: String, lastName: String): Step1Validation {
        val f = firstName.trim()
        val m = middleName.trim()
        val l = lastName.trim()

        val firstErr = when {
            f.isBlank() -> "First name is required"
            f.length < 2 -> "First name must be at least 2 characters"
            f.length > 40 -> "First name must be 40 characters or fewer"
            !NAME_REGEX.matches(f) -> "Use valid alphabetic characters only"
            else -> null
        }

        val middleErr = when {
            m.isBlank() -> null
            m.length > 40 -> "Middle name must be 40 characters or fewer"
            !NAME_REGEX.matches(m) -> "Use valid alphabetic characters only"
            else -> null
        }

        val lastErr = when {
            l.isBlank() -> "Last name is required"
            l.length < 2 -> "Last name must be at least 2 characters"
            l.length > 40 -> "Last name must be 40 characters or fewer"
            !NAME_REGEX.matches(l) -> "Use valid alphabetic characters only"
            else -> null
        }

        return Step1Validation(
            firstNameError = firstErr,
            middleNameError = middleErr,
            lastNameError = lastErr,
            isValid = firstErr == null && middleErr == null && lastErr == null
        )
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.length in 5..150 && EMAIL_REGEX.matches(trimmed)
    }

    fun extractDigits(raw: String): String = raw.filter { it.isDigit() }

    fun normalizePhoneNumber(countryCode: String, localNumber: String): String {
        val digits = extractDigits(localNumber)
        if (digits.isBlank()) return ""
        val cleanCode = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
        return "$cleanCode$digits"
    }

    fun isValidPhoneNumber(countryCode: String, localNumber: String): Boolean {
        val digits = extractDigits(localNumber)
        val entry = RegistrationConstants.COUNTRY_CODES.find { it.code == countryCode }
        val minDigits = entry?.minDigits ?: 7
        val maxDigits = entry?.maxDigits ?: 15
        return digits.length in minDigits..maxDigits
    }

    fun validateStep2(email: String, countryCode: String, phoneLocal: String): Step2Validation {
        val trimmedEmail = email.trim()
        val digits = extractDigits(phoneLocal)
        val hasEmail = trimmedEmail.isNotBlank()
        val hasPhone = digits.isNotBlank()

        if (!hasEmail && !hasPhone) {
            return Step2Validation(
                generalError = "Enter an email address or a mobile number to continue.",
                hasEmail = false,
                hasPhone = false,
                isValid = false
            )
        }

        val emailErr = if (hasEmail && !isValidEmail(trimmedEmail)) {
            "Enter a valid Gmail or email address (e.g., name@gmail.com)."
        } else null

        val phoneErr = if (hasPhone && !isValidPhoneNumber(countryCode, digits)) {
            val entry = RegistrationConstants.COUNTRY_CODES.find { it.code == countryCode }
            val expected = if (entry != null && entry.minDigits == entry.maxDigits) {
                "${entry.minDigits} digits for ${entry.countryName}"
            } else {
                "a valid phone number"
            }
            "Enter $expected."
        } else null

        return Step2Validation(
            emailError = emailErr,
            phoneError = phoneErr,
            generalError = null,
            hasEmail = hasEmail,
            hasPhone = hasPhone,
            isValid = emailErr == null && phoneErr == null
        )
    }

    fun evaluatePassword(password: String, confirmPassword: String): PasswordEvaluation {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = SPECIAL_CHAR_REGEX.containsMatchIn(password)
        val matches = password.isNotEmpty() && password == confirmPassword

        val checksPassed = listOf(
            hasMinLength,
            hasUppercase,
            hasLowercase,
            hasDigit,
            hasSpecial
        ).count { it }

        val label = when {
            password.isEmpty() -> "Enter Password"
            checksPassed <= 2 -> "Weak"
            checksPassed == 3 -> "Fair"
            checksPassed == 4 -> "Good"
            password.length >= 12 -> "Very Strong"
            else -> "Strong"
        }

        val error = when {
            password.isEmpty() -> null
            !hasMinLength -> "Password must be at least 8 characters long."
            !hasUppercase -> "Include at least one uppercase letter (A-Z)."
            !hasLowercase -> "Include at least one lowercase letter (a-z)."
            !hasDigit -> "Include at least one number (0-9)."
            !hasSpecial -> "Include at least one special character (!@#\$%^&*)."
            confirmPassword.isNotEmpty() && !matches -> "Confirm password does not match."
            else -> null
        }

        return PasswordEvaluation(
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecial,
            passwordsMatch = matches,
            strengthScore = checksPassed,
            strengthLabel = label,
            errorMessage = error,
            isValid = checksPassed == 5 && matches
        )
    }

    fun calculateAgeFromDob(
        birthYear: Int,
        birthMonthOneBased: Int,
        birthDay: Int,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): Int {
        val currentYear = referenceCalendar.get(Calendar.YEAR)
        val currentMonth = referenceCalendar.get(Calendar.MONTH) + 1
        val currentDay = referenceCalendar.get(Calendar.DAY_OF_MONTH)
        var age = currentYear - birthYear
        if (currentMonth < birthMonthOneBased ||
            (currentMonth == birthMonthOneBased && currentDay < birthDay)
        ) {
            age -= 1
        }
        return age
    }

    fun computeAgeGroup(age: Int): String = when {
        age < 13 -> "<13"
        age in 13..17 -> "13-17"
        age in 18..24 -> "18-24"
        age in 25..34 -> "25-34"
        age in 35..44 -> "35-44"
        else -> "45+"
    }

    fun validateStep4(
        age: Int?,
        purpose: String,
        otherPurpose: String,
        interests: Set<String>
    ): Step4Validation {
        val ageError = when {
            age == null -> "Please select your date of birth or enter your age."
            age < RegistrationConstants.MIN_ALLOWED_AGE ->
                "You must be at least ${RegistrationConstants.MIN_ALLOWED_AGE} years old to create a Zentrix Store account."
            age > 110 -> "Please enter a valid age."
            else -> null
        }

        val privacyNotice = if (age != null && age in 13..17) {
            "Youth privacy protection enabled: sensitive tracking is disabled and guardian guidance is recommended for purchases."
        } else null

        val purposeError = when {
            purpose.isBlank() || purpose !in RegistrationConstants.PURPOSE_OPTIONS ->
                "Please select your primary purpose."
            purpose == "Other" && otherPurpose.trim().length < 3 ->
                "Please describe your purpose (at least 3 characters)."
            else -> null
        }

        val interestsError = if (interests.isEmpty()) {
            "Select at least one area of interest."
        } else null

        val ageGroup = if (age != null && age >= RegistrationConstants.MIN_ALLOWED_AGE) {
            computeAgeGroup(age)
        } else ""

        return Step4Validation(
            ageError = ageError,
            privacyNotice = privacyNotice,
            purposeError = purposeError,
            interestsError = interestsError,
            resolvedAge = age,
            ageGroup = ageGroup,
            isValid = ageError == null && purposeError == null && interestsError == null
        )
    }

    fun generatePublicUserId(firebaseUid: String): String {
        val cleaned = firebaseUid.filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
        val suffix = cleaned.take(8).ifBlank { "USER0001" }
        return "ZX-$suffix"
    }

    fun contactLockDocumentId(contactType: String, normalizedValue: String): String {
        val prefix = if (contactType == "EMAIL") "em" else "ph"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedValue.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "${prefix}_${hex.take(40)}"
    }
}
