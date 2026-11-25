package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun CatalogueApp(viewModel: CatalogueViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            state.selectedProduct != null -> "Product Details"
                            state.selectedCategory != null -> state.selectedCategory!!.replaceFirstChar { it.uppercase() }
                            state.searchResults != null -> "Search Results"
                            else -> "Product Catalogue"
                        }
                    )
                },
                navigationIcon = if (state.selectedProduct != null || state.selectedCategory != null || state.searchResults != null) {
                    {
                        IconButton(onClick = {
                            if (state.selectedProduct != null) viewModel.closeProductDetails()
                            else viewModel.clearFilters()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else null
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.errorMessage!!, color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadHomeData() }) {
                            Text("Retry")
                        }
                    }
                }
                state.selectedProduct != null -> {
                    ProductDetailView(
                        product = state.selectedProduct!!,
                        confirmationMessage = state.enquiryConfirmation,
                        onEnquireClick = { viewModel.setEnquiryDialogOpen(true) }
                    )
                }
                else -> {
                    BrowseView(
                        state = state,
                        onQueryChange = viewModel::updateSearchQuery,
                        onSearchClick = viewModel::executeSearch,
                        onClearSearch = viewModel::clearFilters,
                        onCategoryClick = viewModel::selectCategory,
                        onProductClick = viewModel::openProductDetails
                    )
                }
            }

            if (state.isEnquiryDialogOpen && state.selectedProduct != null) {
                EnquiryDialog(
                    product = state.selectedProduct!!,
                    onDismiss = { viewModel.setEnquiryDialogOpen(false) },
                    onSubmit = { name, email, msg -> viewModel.submitEnquiry(name, email, msg) }
                )
            }
        }
    }
}

@Composable
private fun BrowseView(
    state: CatalogueUiState,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onProductClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar + Search Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search products...") },
                singleLine = true,
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSearchClick,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Links
        if (state.categories.isNotEmpty()) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    val isSelected = state.selectedCategory == category
                    OutlinedButton(
                        onClick = { onCategoryClick(category) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                        )
                    ) {
                        Text(category.replaceFirstChar { it.uppercase() })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content List: Search Results, Category View, or Home Featured (5 items)
        when {
            state.searchResults != null -> {
                Text(
                    text = "Matching Products (${state.searchResults.size})",
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.searchResults.isEmpty()) {
                    Text("No products matched \"${state.searchQuery}\".")
                } else {
                    ProductList(products = state.searchResults, onProductClick = onProductClick)
                }
            }
            state.selectedCategory != null -> {
                val topRated = state.categoryProducts.take(2)
                val additional = state.categoryProducts.drop(2)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            text = "Top Rated in ${state.selectedCategory.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.h6
                        )
                    }
                    items(topRated) { product ->
                        ProductTile(product = product, badge = "Top Rated", onClick = { onProductClick(product.id) })
                    }
                    if (additional.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Additional Products",
                                style = MaterialTheme.typography.h6
                            )
                        }
                        items(additional) { product ->
                            ProductTile(product = product, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = "Featured Products (Top 5 Rated)",
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductList(products = state.featuredProducts, onProductClick = onProductClick)
            }
        }
    }
}

@Composable
private fun ProductList(products: List<Product>, onProductClick: (Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(products) { product ->
            ProductTile(product = product, onClick = { onProductClick(product.id) })
        }
    }
}

@Composable
private fun ProductTile(
    product: Product,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        elevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image,
                contentDescription = product.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.category.uppercase(),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                    if (badge != null) {
                        Text(
                            text = "★ $badge",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$${"%.2f".format(product.price)}",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "★ ${product.rating.rate} (${product.rating.count})",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailView(
    product: Product,
    confirmationMessage: String?,
    onEnquireClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        AsyncImage(
            model = product.image,
            contentDescription = product.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = product.category.uppercase(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = product.title,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$${"%.2f".format(product.price)}",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Rating: ${product.rating.rate} / 5 (${product.rating.count} reviews)",
                style = MaterialTheme.typography.body2
            )
        }
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = "Description",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = product.description,
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (confirmationMessage != null) {
            Card(
                backgroundColor = Color(0xFFDCFCE7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = confirmationMessage,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onEnquireClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Enquire")
        }
    }
}

@Composable
private fun EnquiryDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enquire about ${product.title}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Your Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; validationError = null },
                    label = { Text("Email Address *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank() || !email.contains("@")) {
                    validationError = "Please enter your name and a valid email."
                } else {
                    onSubmit(name.trim(), email.trim(), message.trim())
                }
            }) {
                Text("Submit Enquiry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}