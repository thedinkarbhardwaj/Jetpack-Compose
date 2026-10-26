package com.sunpawtechnologies.supawui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunpawtechnologies.supawui.R
import com.sunpawtechnologies.supawui.ThemeManager
import com.sunpawtechnologies.supawui.naviagtion.NavigationName
import com.sunpawtechnologies.supawui.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavHostController, productName: String) {

    var expanded by remember { mutableStateOf(false) }
    val items = listOf("Food", "Drinks", "Other")
    var selectedItem by remember { mutableStateOf("") }

    var barCode by remember { mutableStateOf("") }

    var productName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, "")

            }
            Text("$productName", style = TextStyle(color = Color.Black))
            Spacer(Modifier.width(1.dp))

        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(), thickness = 2.dp,
            color = Color.Gray
        )

        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                MainHeading("Select Store")

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = {}, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.Default.Add, "", tint = Primary
                        )

                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Add New", style = TextStyle(color = Primary),
                        modifier = Modifier.clickable{
                            ThemeManager.changeTheme(
                                Color(0xFF1976D2)
                            )
                        })
                }

            }

            LabelledtextField("Select Store")

            ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }) {

                OutlinedTextField(
                    value = selectedItem,
                    onValueChange = {

                    },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
                    expanded = false
                }) {

                    items.forEach { item ->

                        DropdownMenuItem(
                            text = {
                                Text(item)
                            },
                            onClick = {
                                selectedItem = item
                                expanded = false
                            }
                        )
                    }
                }

            }


        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 4.dp,
            color = Color.LightGray
        )

        Column(modifier = Modifier
            .fillMaxWidth().background(color = ThemeManager.primaryColor.value)
            .padding(10.dp)) {

            MainHeading("Add product Details")
            LabelledtextField("Add product image")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                CardImageLay(R.drawable.logo, "Camera", modifier = Modifier.weight(1f), {})
                CardImageLay(R.drawable.logo, "Upload", modifier = Modifier.weight(1f), {})
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "* Backgorund image will be removed automatically",
                style = TextStyle(color = Color.Red)
            )


            LabelledtextField("Barcode")

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    modifier = Modifier.weight(1f), value = barCode,
                    onValueChange = {
                        barCode = it
                    })

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(Icons.Default.AddCircle,"", tint = Primary)
                   Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.scan_barcode), style =
                        TextStyle(color = Primary)
                    )
                }


            }

            BrandCategoryDropdown()

            AddProduct(
                productName,
                onProductNameChange = {
                    productName = it
                }
            )
            InventoryUI()
            VariantsUI(navController)

            Spacer(Modifier.height(20.dp))

            ElevatedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                )) {

                Text("Save")
            }
        }
    }
}

@Composable
fun VariantsUI(navController: NavHostController) {

    Column() {

        Spacer(Modifier.height(20.dp))
        MainHeading("Variant")

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable{
                navController.navigate(NavigationName.addOption)
            }) {
            Icon(Icons.Default.Add,"",)
            Text("Add option (Size,color and flavor etc.)")
        }

    }
}

@Composable
fun InventoryUI() {
    Spacer(Modifier.height(10.dp))
    MainHeading("Inventory")
    Spacer(Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {

        LabelledtextField("Inventory")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically) {

            Icon(Icons.Default.List,"")
            Card(
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0"
                    )
                }
            }
            Icon(Icons.Default.Add,"")

        }
    }
}

@Composable
fun AddProduct(productName: String, onProductNameChange: (String) -> Unit) {


    LabelledtextField("Product Name")

    OutlinedTextField( modifier = Modifier.fillMaxWidth(), value = productName, onValueChange = {
        onProductNameChange(it)
    })
}

@Composable
fun CardImageLay(image: Int, text: String, modifier: Modifier = Modifier, onclick: () -> Unit) {

    Card(
        onClick = onclick,
        modifier = modifier, colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        border = BorderStroke(width = 2.dp, color = Color.Black)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(image), "",
                modifier = Modifier.size(60.dp)
            )
            Text(
                text, style = TextStyle(
                    color = Primary, fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                ),
                modifier = Modifier.offset(y = -20.dp)
            )

        }

    }
}

@Composable
fun MainHeading(text: String) {
    Text(
        text, style = TextStyle(
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun LabelledtextField(title: String) {

    Spacer(Modifier.height(10.dp))
    Text(
        title.uppercase(), style = TextStyle(
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp
        )
    )
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandCategoryDropdown2() {

    val brands = listOf(
        "Nike",
        "Adidas",
        "Puma"
    )

    val categoriesByBrand = mapOf(
        "Nike" to listOf("Shoes", "T-Shirts", "Caps"),
        "Adidas" to listOf("Shoes", "Tracksuits", "Socks"),
        "Puma" to listOf("Shoes", "T-Shirts", "Bags")
    )

    var selectedBrand by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("")
    }

    var brandExpanded by remember {
        mutableStateOf(false)
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

    // Current brand ki categories
    val categories = categoriesByBrand[selectedBrand] ?: emptyList()


    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        // ---------------- BRAND ----------------

        Text(
            text = "PRODUCT BRAND"
        )

        ExposedDropdownMenuBox(
            expanded = brandExpanded,
            onExpandedChange = {
                brandExpanded = !brandExpanded
            }
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                value = selectedBrand,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text("Select Brand")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = brandExpanded
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = brandExpanded,
                onDismissRequest = {
                    brandExpanded = false
                }
            ) {

                brands.forEach { brand ->

                    DropdownMenuItem(
                        text = {
                            Text(brand)
                        },
                        onClick = {

                            selectedBrand = brand

                            // Brand change hote hi category reset
                            selectedCategory = ""

                            brandExpanded = false
                        }
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ---------------- CATEGORY ----------------

        Text(
            text = "PRODUCT CATEGORY"
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = {

                // Brand select nahi hai to dropdown mat kholo
                if (selectedBrand.isNotEmpty()) {
                    categoryExpanded = !categoryExpanded
                }
            }
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                enabled = selectedBrand.isNotEmpty(),
                placeholder = {
                    Text("Select Category")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = categoryExpanded
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = {
                    categoryExpanded = false
                }
            ) {

                categories.forEach { category ->

                    DropdownMenuItem(
                        text = {
                            Text(category)
                        },
                        onClick = {

                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandCategoryDropdown() {

    var selectBrand by remember { mutableStateOf("Select your brand") }
    var expandedBrand by remember { mutableStateOf(false) }
    val brands = listOf("Nike", "Adidas", "Shoes")

    var expandedCategory by remember { mutableStateOf(false) }
    var selectCategory by remember { mutableStateOf("Select your category") }

    val categoriesByBrand = mapOf(
        "Nike" to listOf("Shoes", "T-Shirts", "Caps"),
        "Adidas" to listOf("Shoes", "Tracksuits", "Socks"),
        "Puma" to listOf("Shoes", "T-Shirts", "Bags")
    )
    val category = categoriesByBrand[selectBrand]?:emptyList()


    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    LabelledtextField("Product Brand")

        ExposedDropdownMenuBox(
            expanded = expandedBrand,
            onExpandedChange = {
                expandedBrand = ! expandedBrand
            }
        ) {
            OutlinedTextField(value = selectBrand, onValueChange = {

            } ,readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand)
                })

            ExposedDropdownMenu(
                expanded = expandedBrand,
                onDismissRequest = {
                    expandedBrand= false
                }
            ) {

                brands.forEach {item->

                    DropdownMenuItem(
                        text = {
                            Text(item)
                        },onClick = {
                            selectBrand = item
                            expandedBrand = false
                        })

                }

            }


        }

        LabelledtextField("Product Category")

        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            expanded = expandedCategory,
            onExpandedChange = {
                expandedCategory = !expandedCategory
            }) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                value = selectCategory, onValueChange = {
            },
                readOnly = true)

            ExposedDropdownMenu(expanded = expandedCategory,
                onDismissRequest = {
                    expandedCategory = false
                }) {


                category.forEach {item->

                    DropdownMenuItem(onClick = {
                        selectCategory = item
                        expandedCategory = false
                    },
                        text = {

                            Text(item)
                        })

                }


            }

        }


    }

}