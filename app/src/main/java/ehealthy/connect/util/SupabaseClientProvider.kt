package ehealthy.connect.util

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://gqyhkccupbeudenvsdsf.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxeWhrY2N1cGJldWRlbnZzZHNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MDgyMjgsImV4cCI6MjA5MTA4NDIyOH0.C6dtGH1277KKiMBpXtWSxRY9JQrfbbo7eYKIgomoap8"
    ) {
        install(Auth)
        install(Postgrest)
    }
}

