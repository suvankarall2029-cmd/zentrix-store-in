package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RegistrationValidatorTest {

    @Test
    fun step1_formatsNamesAndRequiresFirstAndLastName() {
        assertEquals("Anirban Kumar Roy", RegistrationValidator.buildFullName(" anirban ", "kumar ", " ROY"))
        assertFalse(RegistrationValidator.validateStep1("", "", "Roy").isValid)
        assertFalse(RegistrationValidator.validateStep1("Anirban", "", "").isValid)
        assertTrue(RegistrationValidator.validateStep1("Anirban", "", "Roy").isValid)
        assertTrue(RegistrationValidator.validateStep1("Anirban", "Kumar", "Roy").isValid)
    }

    @Test
    fun step2_validatesEmailOrPhoneOrBoth() {
        assertFalse(RegistrationValidator.validateStep2("", "+91", "").isValid)
        assertFalse(RegistrationValidator.validateStep2("bad-email", "+91", "").isValid)
        assertTrue(RegistrationValidator.validateStep2("user@gmail.com", "+91", "").isValid)
        assertFalse(RegistrationValidator.validateStep2("", "+91", "12345").isValid)
        assertTrue(RegistrationValidator.validateStep2("", "+91", "9876543210").isValid)
        assertTrue(RegistrationValidator.validateStep2("user@gmail.com", "+91", "9876543210").isValid)
        assertEquals("+919876543210", RegistrationValidator.normalizePhoneNumber("+91", "98765-43210"))
    }

    @Test
    fun step3_enforcesAllPasswordStrengthAndMatchRules() {
        val weak = RegistrationValidator.evaluatePassword("abc123", "abc123")
        assertFalse(weak.isValid)
        assertFalse(weak.hasMinLength)

        val noSpecial = RegistrationValidator.evaluatePassword("Password123", "Password123")
        assertFalse(noSpecial.isValid)
        assertFalse(noSpecial.hasSpecialChar)

        val mismatch = RegistrationValidator.evaluatePassword("Strong@123", "Strong@124")
        assertFalse(mismatch.isValid)
        assertFalse(mismatch.passwordsMatch)

        val strong = RegistrationValidator.evaluatePassword("Zentrix@2026!", "Zentrix@2026!")
        assertTrue(strong.isValid)
        assertEquals(5, strong.strengthScore)
        assertNull(strong.errorMessage)
    }

    @Test
    fun step4_validatesAgeRestrictionsPurposeAndInterests() {
        val ref = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12) }
        val age24 = RegistrationValidator.calculateAgeFromDob(2002, 5, 10, ref)
        assertEquals(24, age24)
        assertEquals("18-24", RegistrationValidator.computeAgeGroup(age24))

        val underage = RegistrationValidator.validateStep4(12, "Student", "", setOf("Python"))
        assertFalse(underage.isValid)
        assertNotNull(underage.ageError)

        val teen = RegistrationValidator.validateStep4(15, "Student", "", setOf("Python"))
        assertTrue(teen.isValid)
        assertNotNull(teen.privacyNotice)

        val missingOther = RegistrationValidator.validateStep4(22, "Other", "x", setOf("Python"))
        assertFalse(missingOther.isValid)
        assertNotNull(missingOther.purposeError)

        val validOther = RegistrationValidator.validateStep4(22, "Other", "Teaching workshops", setOf("Online Courses"))
        assertTrue(validOther.isValid)
        assertEquals("18-24", validOther.ageGroup)
    }
}
