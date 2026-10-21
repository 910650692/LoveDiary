// Ours 占位素材生成器（本地跑，产物直接写进 Assets.xcassets）
// 用法：cd ios && swift scripts/make-placeholder-assets.swift
// 说明：这是构建期辅助脚本，不属于 App 代码；图标/启动标有正式设计后可整份替换
import AppKit

let outDir = FileManager.default.currentDirectoryPath + "/Ours/Assets.xcassets"

// 主题色与 Ours/Theme.swift 保持一致
let primary = NSColor(srgbRed: 1.0, green: 0.36, blue: 0.48, alpha: 1)
let deep    = NSColor(srgbRed: 0.86, green: 0.20, blue: 0.34, alpha: 1)
let gold    = NSColor(srgbRed: 0.95, green: 0.71, blue: 0.33, alpha: 1)

func heartPath(in r: NSRect) -> NSBezierPath {
    let p = NSBezierPath()
    let x = r.minX, y = r.minY, w = r.width, h = r.height
    p.move(to: NSPoint(x: x + w / 2, y: y))
    p.curve(to: NSPoint(x: x + w, y: y + h * 0.62),
            controlPoint1: NSPoint(x: x + w * 0.74, y: y + h * 0.26),
            controlPoint2: NSPoint(x: x + w, y: y + h * 0.34))
    p.curve(to: NSPoint(x: x + w / 2, y: y + h * 0.92),
            controlPoint1: NSPoint(x: x + w, y: y + h * 1.03),
            controlPoint2: NSPoint(x: x + w * 0.62, y: y + h * 1.03))
    p.curve(to: NSPoint(x: x, y: y + h * 0.62),
            controlPoint1: NSPoint(x: x + w * 0.38, y: y + h * 1.03),
            controlPoint2: NSPoint(x: x, y: y + h * 1.03))
    p.curve(to: NSPoint(x: x + w / 2, y: y),
            controlPoint1: NSPoint(x: x, y: y + h * 0.34),
            controlPoint2: NSPoint(x: x + w * 0.26, y: y + h * 0.26))
    p.close()
    return p
}

func render(size: Int, opaque: Bool, draw: (NSRect) -> Void) -> NSBitmapImageRep {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size,
                               bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
                               isPlanar: false, colorSpaceName: .deviceRGB,
                               bytesPerRow: 0, bitsPerPixel: 0)!
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = ctx
    let full = NSRect(x: 0, y: 0, width: size, height: size)
    if opaque {
        NSColor.white.setFill()
        full.fill()
    }
    draw(full)
    NSGraphicsContext.restoreGraphicsState()
    return rep
}

func writePNG(_ rep: NSBitmapImageRep, to path: String) {
    guard let data = rep.representation(using: .png, properties: [:]) else { return }
    try? data.write(to: URL(fileURLWithPath: path))
    print("wrote \(path) (\(rep.pixelsWide)x\(rep.pixelsHigh))")
}

// App 图标 1024（不透明，App Store 要求无 alpha）
let icon = render(size: 1024, opaque: true) { r in
    NSGradient(starting: primary, ending: deep)!.draw(in: r, angle: -70)
    let glow = NSGradient(colors: [NSColor(white: 1, alpha: 0.30), NSColor(white: 1, alpha: 0)])!
    glow.draw(in: NSRect(x: 0, y: r.height * 0.45, width: r.width, height: r.height * 0.55), angle: -90)
    let heartRect = NSRect(x: 232, y: 250, width: 560, height: 520)
    NSColor(white: 1, alpha: 0.18).setFill()
    heartPath(in: heartRect.offsetBy(dx: 0, dy: 14)).fill()
    NSColor.white.setFill()
    heartPath(in: heartRect).fill()
    gold.setFill()
    NSBezierPath(ovalIn: NSRect(x: 636, y: 690, width: 54, height: 54)).fill()
}
writePNG(icon, to: outDir + "/AppIcon.appiconset/AppIcon-1024.png")

// 启动标（透明底：心 + Ours）
for (scale, name) in [(1, "@1x"), (2, "@2x"), (3, "@3x")] {
    let rep = render(size: 200 * scale, opaque: false) { r in
        let s = CGFloat(scale)
        primary.setFill()
        heartPath(in: NSRect(x: 56 * s, y: 78 * s, width: 88 * s, height: 82 * s)).fill()
        let text = NSAttributedString(string: "Ours", attributes: [
            .font: NSFont.systemFont(ofSize: 38 * s, weight: .bold),
            .foregroundColor: primary,
        ])
        let size = text.size()
        text.draw(at: NSPoint(x: (r.width - size.width) / 2, y: 22 * s))
    }
    writePNG(rep, to: outDir + "/LaunchMark.imageset/LaunchMark\(name).png")
}
