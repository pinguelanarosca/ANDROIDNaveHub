package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.IsolationAuditReport
import com.example.domain.model.IsolationCriterionResult
import com.example.domain.model.ProfileDiagnostics
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun IsolationAuditDialog(
    report: IsolationAuditReport?,
    diagnostics: ProfileDiagnostics?,
    isRunning: Boolean,
    onRunAuditClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .testTag("isolation_audit_dialog"),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Segurança",
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Auditoria Técnica do NaveHub",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Isolamento Nativo por Perfil & Validação Multicamadas",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_audit_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Profile Diagnostics Pill Card (BLOQUEIO 17)
                if (diagnostics != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131B2E))
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .testTag("profile_diagnostics_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DETECÇÃO TÉCNICA DE PERFIL ATIVO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanNeon
                                )
                                Text(
                                    text = if (diagnostics.isMultiProfileSupported) "MULTI_PROFILE: SUPORTADO" else "MULTI_PROFILE: NÃO SUPORTADO (FALLBACK)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diagnostics.isMultiProfileSupported) SuccessGreen else Color(0xFFFFAB00)
                                )
                            }
                            Text(
                                text = "Perfil: ${diagnostics.profileName} | Instância: ${diagnostics.webViewId}",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Status: ${diagnostics.profileStatus} (Default Profile Proibido)",
                                fontSize = 10.sp,
                                color = if (diagnostics.profileStatus.contains("FAIL")) ErrorRed else Color(0xFF94A3B8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Status Banner Card
                val passedCount = report?.passedCount ?: 0
                val totalCount = report?.totalCount ?: 20
                val isAllPassed = report?.allPassed == true

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isAllPassed) SuccessGreen.copy(alpha = 0.15f)
                            else if (report != null) ErrorRed.copy(alpha = 0.15f)
                            else Color(0xFF1E293B)
                        )
                        .border(
                            1.dp,
                            if (isAllPassed) SuccessGreen else if (report != null) ErrorRed else CyberBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (report == null) "Auditoria Pendente de Execução"
                                else if (isAllPassed) "ISOLAMENTO VALIDADO (TODOS PASS)"
                                else "FALHAS ENCONTRADAS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isAllPassed) SuccessGreen else if (report != null) ErrorRed else CyanNeon
                            )
                            Text(
                                text = if (report != null) "Aprovados: $passedCount / $totalCount critérios"
                                else "Execute para auditar WebKit Profiles, Room, Cache, SW e Auth",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Button(
                            onClick = onRunAuditClick,
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("run_audit_test_button")
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Executar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Executar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Criteria
                if (report == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Toque em 'Executar' para auditar os testes de isolamento nativo por perfil e persistência.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(report.criteriaResults) { result ->
                            CriterionItemCard(result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CriterionItemCard(result: IsolationCriterionResult) {
    val statusColor = if (result.passed) SuccessGreen else ErrorRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131B2E))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("criterion_result_${result.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (result.passed) "Aprovado" else "Reprovado",
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${result.category}]",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(
                            text = "${result.id}. ${result.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (result.passed) "PASS" else "FAIL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                }

                Text(
                    text = result.description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = result.details,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(top = 3.dp)
                )

                if (result.evidence.isNotBlank()) {
                    Text(
                        text = "Evidência: ${result.evidence}",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
