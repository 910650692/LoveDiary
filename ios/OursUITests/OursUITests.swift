import XCTest

/// 第一批 UI 回归：真的在模拟器上点按 App（simctl 装包/截图做不到这件事）。
///
/// 稳定性约定：
/// - 一律 `waitForExistence`，不用 `sleep`
/// - 断言只依赖中文文案和无障碍标识，不依赖网络返回的数据
/// - T3 用 `-UITEST_FAKE_SESSION`（App 里 #if DEBUG 的后门）直接进「已绑定」状态，不碰云端
///
/// 元素形态备注（iPhone Duo / iOS 27.1 实测）：SwiftUI `TabView` 不是 `app.tabBars`，
/// 5 个 Tab 暴露成 `Toolbar` 里的普通 Button，label 就是「主页 / 纪念日 / 心愿单 / 我们 / 照片」，
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

        let tabLabels = ["主页", "纪念日", "心愿单", "我们", "照片"]
        XCTAssertTrue(app.buttons["主页"].waitForExistence(timeout: 30), "主界面没出来（DEBUG 后门没生效？）")
        for label in tabLabels {
            XCTAssertTrue(app.buttons[label].exists, "缺少 Tab：\(label)")
        }
        // 主页此时没有别的按钮，正好用它数 Tab 数量
        XCTAssertEqual(app.buttons.count, tabLabels.count, "Tab 数量不是 \(tabLabels.count)")

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

        tapTab("照片", in: app)
        XCTAssertTrue(app.navigationBars["照片"].waitForExistence(timeout: 10), "没切到照片页")
        XCTAssertTrue(app.buttons["photos.picker"].exists, "照片页缺选图入口")
        XCTAssertTrue(app.staticTexts["最近上传"].exists, "照片页缺「最近上传」区块")

        // 切回主页，确认来回切不崩
        tapTab("主页", in: app)
        XCTAssertTrue(app.staticTexts["我们在一起的天数"].waitForExistence(timeout: 10), "切不回主页")
    }

    // MARK: - T4 「我们」页：在一起那天 / 天数 / 登出入口

    func testT4_OursTabShowsTogetherCardAndSignOut() {
        let app = launchFakeSession()
        tapTab("我们", in: app)

        XCTAssertTrue(app.navigationBars["我们"].waitForExistence(timeout: 10), "没切到我们页")

        // 在一起那天：后门假 Couple 的 startedDate 写死 2023-05-20
        XCTAssertTrue(app.staticTexts["从 2023年5月20日 开始"].waitForExistence(timeout: 10),
                      "在一起那天没显示假 startedDate")

        // 天数：只断言「标签在 + 数字区是纯数字」，不写死 1250（这个数每天都会变）
        XCTAssertTrue(app.staticTexts["在一起的天数"].exists, "缺「在一起的天数」标签")
        let days = app.staticTexts["ours.days"]
        XCTAssertTrue(days.exists, "天数区域没渲染")
        XCTAssertNotNil(Int(days.label), "天数区域不是纯数字：「\(days.label)」")

        let signOut = app.buttons["ours.signOut"]
        XCTAssertTrue(signOut.exists, "缺「退出登录」按钮")
        XCTAssertTrue(signOut.label.contains("退出登录"), "登出按钮文案变了：「\(signOut.label)」")
        XCTAssertTrue(signOut.isEnabled, "登出按钮初始就是禁用态")

        // 修改在一起日期：能点开编辑面板就够了（只开不保存，不碰真实数据）
        let editStart = app.buttons["ours.editStartDate"]
        XCTAssertTrue(editStart.exists, "缺「修改在一起日期」入口")
        editStart.tap()
        XCTAssertTrue(app.navigationBars["修改在一起日期"].waitForExistence(timeout: 10),
                      "日期编辑面板没打开")
        XCTAssertTrue(app.buttons["ours.saveStartDate"].exists, "编辑面板缺保存按钮")
        app.buttons["取消"].tap()
        XCTAssertFalse(app.navigationBars["修改在一起日期"].exists, "点取消后面板没关掉")
    }

    // MARK: - T5 登出链路：二次确认 → 回登录页

    func testT5_SignOutFromOursTabReturnsToSignIn() {
        let app = launchFakeSession()
        tapTab("我们", in: app)

        let signOut = app.buttons["ours.signOut"]
        XCTAssertTrue(signOut.waitForExistence(timeout: 10), "「我们」页没加载出登出按钮")
        scrollIntoView(signOut, in: app)
        signOut.tap()

        // 二次确认：SwiftUI confirmationDialog 在 iPhone 上是 action sheet
        let dialog = app.sheets.firstMatch
        XCTAssertTrue(dialog.waitForExistence(timeout: 10), "二次确认对话框没弹出")

        let confirm = dialog.buttons["退出登录"]
        XCTAssertTrue(confirm.waitForExistence(timeout: 5), "确认框里没有「退出登录」按钮")
        // iPhone Duo / iOS 27.1 实测：这个 confirmationDialog 只渲染标题+说明+破坏性按钮，
        // 「取消」不在可访问层级里（点空白处关闭），所以只断言标题与确认按钮
        XCTAssertTrue(dialog.staticTexts["确定要退出登录吗？"].exists, "确认框标题不对")
        confirm.tap()

        // 假 session 下 AppState.signOut 走 DEBUG 短路（不发请求、不碰 OneSignal），应立刻回登录页
        XCTAssertTrue(app.textFields["signin.email"].waitForExistence(timeout: 20), "没回到登录页（缺邮箱输入框）")
        XCTAssertTrue(app.secureTextFields["signin.password"].exists, "登录页缺密码输入框")
        XCTAssertTrue(app.buttons["signin.login"].exists, "登录页缺登录按钮")
        XCTAssertFalse(app.navigationBars["我们"].exists, "登出后还停在「我们」页")
    }

    // MARK: - T6 DEBUG 照片管线自测页

    func testT6_DebugPhotoPipelinePageOpens() {
        let app = launchFakeSession()
        tapTab("我们", in: app)

        let entry = app.buttons["ours.debugPhotos"]
        XCTAssertTrue(entry.waitForExistence(timeout: 10), "「我们」页没有 DEBUG 照片自测入口")
        scrollIntoView(entry, in: app)
        entry.tap()

        XCTAssertTrue(app.navigationBars["照片管线自测"].waitForExistence(timeout: 10), "自测页没打开")
        XCTAssertTrue(app.buttons["photosDebug.picker"].waitForExistence(timeout: 10),
                      "自测页缺「选择一张照片」按钮")
        XCTAssertTrue(app.staticTexts["选一张照片，看压缩后的尺寸、体积与耗时"].exists, "自测页初始状态文案不对")

        // 故意不点选图按钮：PhotosPicker 会拉起系统相册，UI 测试不去操作那个选择器
    }

    // MARK: - 工具

    /// 用 DEBUG 后门的假 session 启动：本地假 Couple，直接进「已绑定」，不碰云端
    private func launchFakeSession() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += [
            // App 内 #if DEBUG 后门：本地假 Couple，直接进「已绑定」
            "-UITEST_FAKE_SESSION",
            // 别弹系统推送权限（PushRouter 靠这个 UserDefaults 标记只问一次），否则挡住点击
            "-push.permissionAsked", "YES",
        ]
        app.launch()
        XCTAssertTrue(app.buttons["主页"].waitForExistence(timeout: 30), "主界面没出来（DEBUG 后门没生效？）")
        return app
    }

    // MARK: - T7 真注册（会写真实数据，默认不编译）
#if REAL_SIGNUP

    /// 走真实 Supabase 的注册链路，验证「注册成功 → 直接进绑定页」。
    ///
    /// ⚠️ 这条会**真的在项目里创建一个账号**（邮箱随机），所以用编译期开关隔离：
    /// 默认不参与编译，要跑必须显式加标志：
    ///   xcodebuild test ... OTHER_SWIFT_FLAGS='$(inherited) -D REAL_SIGNUP' \
    ///     -only-testing:OursUITests/OursUITests/testT7_RealSignupLandsOnBind
    /// 跑完去 Dashboard → Authentication → Users 删掉 `uitest+…@ourstest.app`。
    func testT7_RealSignupLandsOnBind() throws {
        let email = "uitest+\(Int(Date().timeIntervalSince1970))@ourstest.app"
        let app = XCUIApplication()
        app.launchArguments += ["-UITEST_SIGNED_OUT"]
        app.launch()

        let entry = app.buttons["signin.signup"]
        XCTAssertTrue(entry.waitForExistence(timeout: 30), "注册入口没出现")
        entry.tap()

        let nickname = app.textFields["signup.nickname"]
        XCTAssertTrue(nickname.waitForExistence(timeout: 10), "注册页没打开")
        nickname.tap(); nickname.typeText("测试")
        app.textFields["signup.email"].tap(); app.textFields["signup.email"].typeText(email)
        app.secureTextFields["signup.password"].tap()
        app.secureTextFields["signup.password"].typeText("ours123456")

        app.buttons["signup.submit"].tap()

        // 关键断言：注册成功后应当离开注册页、进入绑定页（而不是停在「去邮箱确认」）
        let bindTitle = app.staticTexts["欢迎加入 Ours 🎈"]
        _ = bindTitle.waitForExistence(timeout: 40)
        if !bindTitle.exists {
            print("=== 注册后卡住的界面层级 ===\n\(app.debugDescription)")
        }
        XCTAssertTrue(bindTitle.exists, "注册后没进绑定页（停在注册页或报了错）。邮箱：\(email)")
        XCTAssertFalse(app.buttons["signup.submit"].exists, "注册页还开着，说明没跳走")
    }
#endif

    /// 元素在滚动容器里可能初始不可见：用滑动代替 sleep，最多 4 次
    private func scrollIntoView(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<4 {
            if element.isHittable { return }
            app.swipeUp()
        }
    }

    /// 点某个 Tab 并断言它真的成了选中态（SwiftUI 给选中 Tab 打 Selected trait）
    private func tapTab(_ label: String, in app: XCUIApplication) {
        let button = app.buttons[label]
        XCTAssertTrue(button.waitForExistence(timeout: 10), "Tab 点不到：\(label)")
        button.tap()
        // 选中态是异步落地的，用谓词等待而不是立即断言（整批连跑时上一轮模拟器还没闲下来会偶发失败）
        let selected = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "isSelected == true"), object: button
        )
        XCTAssertEqual(XCTWaiter().wait(for: [selected], timeout: 10), .completed,
                       "点了之后 \(label) 没变成选中态")
    }
}
