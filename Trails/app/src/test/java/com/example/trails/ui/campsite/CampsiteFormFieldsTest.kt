// Unit tests for the campsite form logic (US 02.01.01, 02.01.02 tags, 02.01.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CampsiteFormFieldsTest {

    private lateinit var fields: CampsiteFormFields

    @Before
    fun setUp() {
        fields = CampsiteFormFields(
            name = "  Grande Cache Ridge ",
            locationName = "Hinton - Grande Cache, AB",
            description = "Quiet ridge-top site with an outdoor cookhouse and tennis court.",
            maxOccupancy = "8",
            pricePerNight = "65",
            tags = listOf("Lakeside", "Cookhouse"),
            rules = "Quiet hours 10pm-7am. No generators.",
            cancellationPolicy = "Free cancellation up to 7 days before arrival.",
            latitude = 53.89,
            longitude = -118.01,
            availableDays = setOf(dayKey(2027, 5, 7), dayKey(2027, 5, 8))
        )
    }

    private fun valid(result: CampsiteFormResult): Campsite {
        assertTrue("expected Valid but was $result", result is CampsiteFormResult.Valid)
        return (result as CampsiteFormResult.Valid).campsite
    }

    private fun errors(result: CampsiteFormResult): Map<String, String> {
        assertTrue("expected Invalid but was $result", result is CampsiteFormResult.Invalid)
        return (result as CampsiteFormResult.Invalid).errors
    }

    @Test
    fun toCampsite_validNewCampsite_parsedAndTrimmed() {
        val c = valid(fields.toCampsite(base = null, ownerId = "owner-01"))
        assertEquals("", c.id)
        assertEquals("owner-01", c.ownerId)
        assertEquals("Grande Cache Ridge", c.name)
        assertEquals(8, c.maxOccupancy)
        assertEquals(65.0, c.pricePerNight, 0.0)
        assertEquals(listOf("Lakeside", "Cookhouse"), c.tags)
    }

    @Test
    fun toCampsite_priceWithDollarSign_parsed() {
        val c = valid(fields.copy(pricePerNight = "$42.50").toCampsite(null, "owner-01"))
        assertEquals(42.5, c.pricePerNight, 0.0)
    }

    @Test
    fun toCampsite_blankOccupancy_requiredError() {
        val e = errors(fields.copy(maxOccupancy = "").toCampsite(null, "owner-01"))
        assertEquals("Max occupancy is required", e["maxOccupancy"])
    }

    @Test
    fun toCampsite_nonNumericOccupancy_wholeNumberError() {
        val e = errors(fields.copy(maxOccupancy = "eight").toCampsite(null, "owner-01"))
        assertEquals("Enter a whole number, e.g. 8", e["maxOccupancy"])
    }

    @Test
    fun toCampsite_zeroOccupancy_modelError() {
        val e = errors(fields.copy(maxOccupancy = "0").toCampsite(null, "owner-01"))
        assertEquals(setOf("maxOccupancy"), e.keys)
    }

    @Test
    fun toCampsite_badPrice_priceError() {
        val e = errors(fields.copy(pricePerNight = "cheap").toCampsite(null, "owner-01"))
        assertEquals(setOf("pricePerNight"), e.keys)
    }

    @Test
    fun toCampsite_blankRequiredText_allReported() {
        val e = errors(CampsiteFormFields(maxOccupancy = "4", pricePerNight = "30").toCampsite(null, "owner-01"))
        assertEquals(
            setOf("name", "locationName", "description", "rules", "cancellationPolicy", "location", "availability"),
            e.keys
        )
    }

    @Test
    fun toCampsite_editing_keepsFieldsTheFormDoesNotShow() {
        val existing = Campsite(
            id = "campsite-hinton", ownerId = "owner-01", name = "Old name",
            locationName = "Hinton, AB", latitude = 53.89, longitude = -118.01,
            maxOccupancy = 6, pricePerNight = 50.0, imageUrl = "https://example.com/ridge.jpg",
            availability = listOf(DateRange()), ratingAverage = 4.6, ratingCount = 23,
            activeLotteryId = "lottery-may"
        )
        val c = valid(fields.copy(imageUrl = existing.imageUrl).toCampsite(base = existing, ownerId = "someone-else"))
        assertEquals("campsite-hinton", c.id)
        assertEquals("owner-01", c.ownerId)
        assertEquals(53.89, c.latitude, 0.0)
        assertEquals("https://example.com/ridge.jpg", c.imageUrl)
        assertEquals(1, c.availability.size)
        assertEquals(4.6, c.ratingAverage, 0.0)
        assertEquals("lottery-may", c.activeLotteryId)
        assertEquals("Grande Cache Ridge", c.name)
    }

    @Test
    fun toCampsite_pickedPhoto_carriedThrough() {
        val c = valid(fields.copy(imageUrl = "content://media/picker/0/42").toCampsite(null, "owner-01"))
        assertEquals("content://media/picker/0/42", c.imageUrl)
    }

    @Test
    fun toCampsite_photoRemovedWhileEditing_null() {
        val existing = Campsite(id = "c1", ownerId = "owner-01", imageUrl = "https://example.com/ridge.jpg")
        val c = valid(fields.copy(imageUrl = null).toCampsite(existing, "owner-01"))
        assertEquals(null, c.imageUrl)
    }

    @Test
    fun toCampsite_pin_copiedToCampsite() {
        val c = valid(fields.toCampsite(null, "owner-01"))
        assertEquals(53.89, c.latitude, 0.0)
        assertEquals(-118.01, c.longitude, 0.0)
    }

    @Test
    fun toCampsite_noPin_locationError() {
        val e = errors(fields.copy(latitude = null, longitude = null).toCampsite(null, "owner-01"))
        assertEquals(setOf("location"), e.keys)
    }

    @Test
    fun from_campsiteWithPin_prefillsPin() {
        val f = CampsiteFormFields.from(Campsite(latitude = 51.18, longitude = -115.55))
        assertEquals(51.18, f.latitude!!, 0.0)
        assertEquals(-115.55, f.longitude!!, 0.0)
    }

    @Test
    fun from_campsiteWithoutPin_nullPin() {
        val f = CampsiteFormFields.from(Campsite())
        assertEquals(null, f.latitude)
        assertEquals(null, f.longitude)
    }

    @Test
    fun changedFrom_movedPin_reportsLocation() {
        assertEquals(setOf("location"), fields.copy(latitude = 50.0).changedFrom(fields))
    }

    @Test
    fun toCampsite_noDays_availabilityError() {
        val e = errors(fields.copy(availableDays = emptySet()).toCampsite(null, "owner-01"))
        assertEquals("Pick at least one available date", e["availability"])
    }

    @Test
    fun toCampsite_consecutiveDays_oneRange() {
        val c = valid(fields.toCampsite(null, "owner-01"))
        assertEquals(1, c.availability.size)
        assertEquals(fields.availableDays, availabilityToDays(c.availability))
    }

    @Test
    fun from_availability_prefillsDays() {
        val c = Campsite(availability = daysToAvailability(setOf(dayKey(2027, 5, 14))))
        assertEquals(setOf(dayKey(2027, 5, 14)), CampsiteFormFields.from(c).availableDays)
    }

    @Test
    fun changedFrom_toggledDay_reportsAvailability() {
        val edited = fields.copy(availableDays = fields.availableDays + dayKey(2027, 5, 9))
        assertEquals(setOf("availability"), edited.changedFrom(fields))
    }

    @Test
    fun from_existingPhoto_prefilled() {
        val c = Campsite(imageUrl = "https://example.com/ridge.jpg")
        assertEquals("https://example.com/ridge.jpg", CampsiteFormFields.from(c).imageUrl)
    }

    @Test
    fun isLocalImage_contentAndFileTrue_httpsFalse() {
        assertTrue(isLocalImage("content://media/picker/0/42"))
        assertTrue(isLocalImage("file:///sdcard/ridge.jpg"))
        assertTrue(!isLocalImage("https://firebasestorage.googleapis.com/ridge.jpg"))
    }

    @Test
    fun from_existingCampsite_prefillsForm() {
        val c = Campsite(name = "Two Jack Lakeside", locationName = "Banff, AB", maxOccupancy = 4, pricePerNight = 38.0)
        val f = CampsiteFormFields.from(c)
        assertEquals("Two Jack Lakeside", f.name)
        assertEquals("4", f.maxOccupancy)
        assertEquals("38", f.pricePerNight)
    }

    @Test
    fun formatPrice_wholeAndFractional() {
        assertEquals("65", CampsiteFormFields.formatPrice(65.0))
        assertEquals("42.50", CampsiteFormFields.formatPrice(42.5))
    }

    @Test
    fun addTag_trimsAndAppends() {
        assertEquals(listOf("Lakeside", "Tennis"), addTag(listOf("Lakeside"), "  Tennis "))
    }

    @Test
    fun addTag_duplicateIgnoringCase_ignored() {
        assertEquals(listOf("Lakeside"), addTag(listOf("Lakeside"), "lakeside"))
    }

    @Test
    fun addTag_blank_ignored() {
        assertEquals(listOf("Lakeside"), addTag(listOf("Lakeside"), "   "))
    }

    @Test
    fun changedFrom_reportsOnlyEditedFields() {
        val edited = fields.copy(name = "New", pricePerNight = "70")
        assertEquals(setOf("name", "pricePerNight"), edited.changedFrom(fields))
    }
}
