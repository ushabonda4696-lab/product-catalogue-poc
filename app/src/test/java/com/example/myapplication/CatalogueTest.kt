package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueTest {

    private val sampleProducts = listOf(
        Product(1, "Alpha Jacket", 99.0, "Warm winter coat", "men's clothing", "", Rating(4.9, 120)),
        Product(2, "Beta Shirt", 25.0, "Cotton tee", "men's clothing", "", Rating(3.8, 40)),
        Product(3, "Gamma Ring", 150.0, "Silver band", "jewelery", "", Rating(4.7, 85)),
        Product(4, "Delta Watch", 200.0, "Chronograph", "electronics", "", Rating(4.6, 210)),
        Product(5, "Epsilon Bag", 65.0, "Leather tote", "women's clothing", "", Rating(4.5, 95)),
        Product(6, "Zeta Scarf", 18.0, "Silk scarf", "women's clothing", "", Rating(3.2, 15))
    )

    @Test
    fun `featured products returns top 5 sorted by highest rating`() {
        val featured = sampleProducts
            .sortedByDescending { it.rating.rate }
            .take(5)

        assertEquals(5, featured.size)
        assertEquals("Alpha Jacket", featured.first().title)
        assertEquals("Beta Shirt", featured.last().title)
    }

    @Test
    fun `search matches case-insensitively across title and description`() {
        val query = "leather"
        val results = sampleProducts.filter {
            it.title.lowercase().contains(query) || it.description.lowercase().contains(query)
        }

        assertEquals(1, results.size)
        assertEquals(5, results.first().id)
    }

    @Test
    fun `category products sort top rated first followed by additional items`() {
        val sortedCategory = sampleProducts
            .filter { it.category == "men's clothing" }
            .sortedByDescending { it.rating.rate }

        assertEquals(2, sortedCategory.size)
        assertTrue(sortedCategory[0].rating.rate >= sortedCategory[1].rating.rate)
    }
}