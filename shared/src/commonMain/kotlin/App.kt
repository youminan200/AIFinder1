import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    MaterialTheme {
        var count by remember { mutableStateOf(0) }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F5F5)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AIfinder 🚀",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6200EE)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "iOS & Android 동시 업데이트 성공!",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Card(
                    elevation = 4.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("현재 클릭 횟수", fontSize = 14.sp)
                        Text(
                            text = "$count",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF03DAC5)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { count++ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE))
                ) {
                    Text("응원 버튼 누르기!", color = Color.White, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { count = 0 }) {
                    Text("초기화", color = Color.Gray)
                }
            }
        }
    }
}
