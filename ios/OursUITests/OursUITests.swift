import XCTest

/// 第一批 UI 回归：真的在模拟器上点按 App（simctl 装包/截图做不到这件事）。
///
/// 稳定性约定：
/// - 一律 `waitForExistence`，不用 `sleep`
/// - 断言只依赖中文文案和无障碍标识，不依赖网络返回的数据
/// - T3 用 `-UITEST_FAKE_SESSION`（App 里 #if DEBUG 的后门）直接进「已绑定」状态，不碰云端
///
/// 元素形态备注（iPhone Duo / iOS 27.1 实测）：SwiftUI `TabView` 不是 `app.tabBars`，
/// 4 个 Tab 暴露成 `Toolbar` 里的普通 Button，label 就是「主页 / 纪念日 / 心愿单 / 我们」，
/// 所以统一用 `app.buttons[label]` 查（`app.buttons` 会匹配任意容器下的按钮）。
final class OursUITests: XCTestCase {

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    // MARK: - T1 冷启动：登录页关键元素

    func testT1_ColdLaunchShowsSignInElements() {
        let app = XCUIApplication()
        // 强制未登录：模拟器里若残留上一轮的 session，这个用例也必须看到登录页
        app.launchArguments += ["-UITEST_SIGNED_OUT"]
        app.launch()

        XCTAssertTrue(app.staticTexts["Ours"].waitForExistence(timeout: 30), "登录页标题「Ours」没出现")
        XCTAssertTrue(app.staticTexts["我们的空间"].exists, "登录页副标题没出现")

        XCTAssertTrue(app.textFields["signin.email"].exists, "邮箱输入框没出现")
        XCTAssertTrue(app.secureTextFields["signin.password"].exists, "密码输入框没出现")

        let loginButton = app.buttons["signin.login"]
        XCTAssertTrue(loginButton.exists, "登录按钮没出现")
        XCTAssertEqual(loginButton.label, "登录", "登录按钮文案变了")

        XCTAssertTrue(app.buttons["还没有账号？注册"].exists, "「还没有账号？注册」入口没出现")
    }

    // MARK: - T2 注册入口：能进注册页（只进不提交）

    func testT2_SignUpEntryShowsSignUpForm() {
        let app = XCUIApplication()
        app.launchArguments += ["-UITEST_SIGNED_OUT"]
        app.launch()

        let signUpEntry = app.buttons["signin.signup"]
        XCTAssertTrue(signUpEntry.waitForExistence(timeout: 30), "「还没有账号？注册」入口没出现")
        signUpEntry.tap()

        XCTAssertTrue(app.staticTexts["加入 Ours"].waitForExistence(timeout: 10), "注册页没打开")
        XCTAssertTrue(app.textFields["signup.nickname"].exists, "注册页缺昵称输入框")
        XCTAssertTrue(app.textFields["signup.email"].exists, "注册页缺邮箱输入框")
        XCTAssertTrue(app.secureTextFields["signup.password"].exists, "注册页缺密码输入框")

        let submitButton = app.buttons["signup.submit"]
        XCTAssertTrue(submitButton.exists, "注册页缺注册按钮")
        XCTAssertEqual(submitButton.label, "注册", "注册按钮文案变了")

        // 故意不点「注册」：UI 测试不去云端造数据
    }

    // MARK: - T3 主界面 4 个 Tab 存在且能互切

    func testT3_MainTabsExistAndSwitch() {
        let app = XCUIApplication()
        app.launchArguments += [
            // App 内 #if DEBUG 后门：本地假 Couple，直接进「已绑定」
            "-UITEST_FAKE_SESSION",
            // 别弹系统推送权限（PushRouter 靠这个 UserDefaults 标记只问一次），否则挡住点击
            "-push.permissionAsked", "YES",
        ]
        app.launch()

        let tabLabels = ["主页", "纪念日", "心愿单", "我们"]
        XCTAssertTrue(app.buttons["主页"].waitForExistence(timeout: 30), "主界面没出来（DEBUG 后门没生效？）")
        for label in tabLabels {
            XCTAssertTrue(app.buttons[label].exists, "缺少 Tab：\(label)")
        }
        // 主页此时没有别的按钮，正好用它数 Tab 数量
        XCTAssertEqual(app.buttons.count, tabLabels.count, "Tab 数量不是 4")

        // 逐个切过去，每个 Tab 用「选中态 + 该页特有内容」双双确认
        tapTab("主页", in: app)
        XCTAssertTrue(app.staticTexts["我们在一起的天数"].waitForExistence(timeout: 10), "没停在主页")

        tapTab("纪念日", in: app)
        XCTAssertTrue(app.navigationBars["重要日期"].waitForExistence(timeout: 10), "没切到纪念日页")

        tapTab("心愿单", in: app)
        XCTAssertTrue(app.navigationBars["心愿单"].waitForExistence(timeout: 10), "没切到心愿单页")

        tapTab("我们", in: app)
        XCTAssertTrue(app.navigationBars["我们"].waitForExistence(timeout: 10), "没切到我们页")
        // 假 Couple 已绑定：能看到两个人的昵称位和「在一起的天数」，且没有「还没绑定」兜底文案
        XCTAssertTrue(app.staticTexts["我"].exists, "我们页缺「我」的昵称位")
        XCTAssertTrue(app.staticTexts["另一半"].exists, "我们页缺「另一半」的昵称位")
        XCTAssertTrue(app.staticTexts["在一起的天数"].exists, "我们页缺「在一起的天数」")
        XCTAssertTrue(app.staticTexts["从 2023年5月20日 开始"].exists, "后门给的假 startedDate 没生效")
        XCTAssertFalse(app.staticTexts["还没有和另一半绑定，绑定后这里会显示两个人的名字"].exists,
                       "后门没进入已绑定状态")

        // 切回主页，确认来回切不崩
        tapTab("主页", in: app)
        XCTAssertTrue(app.staticTexts["我们在一起的天数"].waitForExistence(timeout: 10), "切不回主页")
    }

    // MARK: - 工具

    /// 点某个 Tab 并断言它真的成了选中态（SwiftUI 给选中 Tab 打 Selected trait）
    private func tapTab(_ label: String, in app: XCUIApplication) {
        let button = app.buttons[label]
        XCTAssertTrue(button.waitForExistence(timeout: 10), "Tab 点不到：\(label)")
        button.tap()
        XCTAssertTrue(button.isSelected, "点了之后 \(label) 没变成选中态")
    }
}
