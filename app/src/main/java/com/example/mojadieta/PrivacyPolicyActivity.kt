package com.example.mojadieta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mojadieta.ui.theme.MojaDietaTheme

/**
 * Wyjaśnienie, jak aplikacja korzysta z danych zdrowotnych.
 * Wymagane przez Health Connect (wyświetlane z poziomu okna zgody).
 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MojaDietaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .systemBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Dane o zdrowiu w aplikacji Moja Dieta", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Moja Dieta odczytuje z Health Connect wyłącznie pomiary wagi ciała – np. z wagi " +
                                "Huawei zsynchronizowanej przez aplikację Huawei Zdrowie.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Pomiary są używane tylko do pokazania historii i wykresu wagi oraz do obliczenia " +
                                "Twojego dziennego zapotrzebowania na kalorie.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Wszystkie dane zostają na Twoim telefonie. Aplikacja nie wysyła danych o zdrowiu do " +
                                "internetu, nie udostępnia ich innym podmiotom i niczego nie zapisuje do Health Connect.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Zgodę możesz w każdej chwili cofnąć w ustawieniach Health Connect.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { finish() }) { Text("Zamknij") }
                    }
                }
            }
        }
    }
}
