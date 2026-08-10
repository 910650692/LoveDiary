import Foundation
import Supabase

enum SupabaseClientProvider {
    static let shared: SupabaseClient = {
        SupabaseClient(
            supabaseURL: URL(string: AppConfig.supabaseURL)!,
            supabaseKey: AppConfig.supabaseAnonKey
        )
    }()
}
