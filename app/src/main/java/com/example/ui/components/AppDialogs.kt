package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed

@Composable
fun AddAccountDialog(
    platformName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var accountName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Adicionar Conta em $platformName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "A nova conta terá ambiente de navegação, cookies e armazenamento 100% isolados das demais.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Nome da Conta (Opcional)") },
                    placeholder = { Text("Ex: Conta Principal, VIP, etc.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_account_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CyberBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(accountName.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_account_confirm_button")
            ) {
                Text("Criar Conta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun RenameAccountDialog(
    account: Account,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var accountName by remember { mutableStateOf(account.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Renomear Conta",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Novo Nome") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_rename_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFCBD5E1),
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CyberBorder
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (accountName.isNotBlank()) onConfirm(accountName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_rename_confirm_button")
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    account: Account,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Remover Conta?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja remover '${account.name}'? Todos os cookies, sessões e armazenamento persistente desta conta serão permanentemente apagados. As outras contas não serão afetadas.",
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                modifier = Modifier.testTag("dialog_delete_confirm_button")
            ) {
                Text("Remover", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun AddPlatformDialog(
    onConfirm: (name: String, url: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val colors = listOf("#00E5FF", "#00E676", "#FFAB00", "#FF4081", "#8B5CF6", "#F97316")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Adicionar Nova Plataforma",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Plataforma") },
                    placeholder = { Text("Ex: BetPro, PortalX") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_platform_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL Inicial") },
                    placeholder = { Text("https://exemplo.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_platform_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFCBD5E1),
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                Text(
                    text = "Cor de Destaque:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalUrl = if (url.isNotBlank()) url else "https://${name.lowercase().trim()}.com"
                        onConfirm(name.trim(), finalUrl, selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_platform_confirm_button")
            ) {
                Text("Adicionar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}
