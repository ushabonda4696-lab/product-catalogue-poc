package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// Repository boundary on the client side pointing ONLY to our backend
class CatalogueRepository(
    private val baseUrl: String = "http://10.0.2.2:3000"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getFeaturedProducts(): List<Product> =
        client.get("$baseUrl/api/products/featured").body()

    suspend fun getCategories(): List<String> =
        client.get("$baseUrl/api/categories").body()

    suspend fun getProductsByCategory(category: String): List<Product> =
        client.get("$baseUrl/api/categories/${category.encodeURLPathPart()}").body()

    suspend fun searchProducts(query: String): List<Product> =
        client.get("$baseUrl/api/products/search") {
            parameter("q", query)
        }.body()

    suspend fun getProductById(id: Int): Product =
        client.get("$baseUrl/api/products/$id").body()

    suspend fun submitEnquiry(request: EnquiryRequest): EnquiryResponse =
        client.post("$baseUrl/api/enquiries") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}

data class CatalogueUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val featuredProducts: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val categoryProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Product>? = null,
    val selectedProduct: Product? = null,
    val isEnquiryDialogOpen: Boolean = false,
    val enquiryConfirmation: String? = null
)

class CatalogueViewModel(
    private val repository: CatalogueRepository = CatalogueRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogueUiState())
    val uiState: StateFlow<CatalogueUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Brief retry loop in case Netty is still binding on first launch
                var featured: List<Product> = emptyList()
                var categories: List<String> = emptyList()
                var lastError: Exception? = null

                repeat(3) { attempt ->
                    try {
                        featured = repository.getFeaturedProducts()
                        categories = repository.getCategories()
                        lastError = null
                        return@repeat
                    } catch (e: Exception) {
                        lastError = e
                        delay(400L * (attempt + 1))
                    }
                }
                if (lastError != null) throw lastError!!

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        featuredProducts = featured,
                        categories = categories,
                        selectedCategory = null,
                        searchResults = null,
                        selectedProduct = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to connect to catalogue backend: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedCategory = category,
                    searchResults = null,
                    selectedProduct = null
                )
            }
            try {
                val items = repository.getProductsByCategory(category)
                _uiState.update { it.copy(isLoading = false, categoryProducts = items) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load category: ${e.localizedMessage}")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun executeSearch() {
        val q = _uiState.value.searchQuery.trim()
        if (q.isEmpty()) {
            clearFilters()
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedCategory = null,
                    selectedProduct = null
                )
            }
            try {
                val results = repository.searchProducts(q)
                _uiState.update { it.copy(isLoading = false, searchResults = results) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Search failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun openProductDetails(productId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, enquiryConfirmation = null) }
            try {
                val product = repository.getProductById(productId)
                _uiState.update { it.copy(isLoading = false, selectedProduct = product) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load product: ${e.localizedMessage}")
                }
            }
        }
    }

    fun closeProductDetails() {
        _uiState.update { it.copy(selectedProduct = null, enquiryConfirmation = null) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCategory = null,
                searchQuery = "",
                searchResults = null,
                selectedProduct = null
            )
        }
    }

    fun setEnquiryDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isEnquiryDialogOpen = open) }
    }

    fun submitEnquiry(name: String, email: String, message: String) {
        val product = _uiState.value.selectedProduct ?: return
        viewModelScope.launch {
            try {
                val response = repository.submitEnquiry(
                    EnquiryRequest(
                        productId = product.id,
                        productTitle = product.title,
                        name = name,
                        email = email,
                        message = message
                    )
                )
                _uiState.update {
                    it.copy(
                        isEnquiryDialogOpen = false,
                        enquiryConfirmation = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEnquiryDialogOpen = false,
                        errorMessage = "Could not send enquiry: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}