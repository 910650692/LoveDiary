import Foundation

/// 项目配置（anon key 是公开的客户端 key，提交无风险）
enum AppConfig {
    // Supabase（supabase/API.md 有对应说明）
    static let supabaseURL = "https://prkijbbjrjfmkolkjxsk.supabase.co"
    static let supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBya2lqYmJqcmpmbWtvbGtqeHNrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU4Mzg5OTUsImV4cCI6MjEwMTQxNDk5NX0.UTyergvjk8TxwfULh4-hLQ3jORM2WM3Cgfw5toOPF88"

    // OneSignal（推送）
    static let oneSignalAppID = "44ec678d-0073-4d2d-817b-a41c61fdbc22"
}
