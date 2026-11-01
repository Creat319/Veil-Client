<div align="center">

# Veil Client

基于 [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) 二次修改的 Fabric 客户端（Minecraft 客户端修改）。
界面、HUD 组件经过重新设计，保留 LiquidBounce 的全部功能与配置体系。

</div>

---

## 二次分发

**本项目是 LiquidBounce 的二次分发（fork / 衍生作品），不是官方版本**
- 上游项目：**LiquidBounce** — <https://github.com/CCBlueX/LiquidBounce>

### 本仓库相对上游的修改

以下修改均为界面 / 显示层改动，功能行为与上游保持一致：

| 位置 | 修改内容 |
|---|---|
| ClickGUI（`src-theme/src/routes/clickgui`） | 整体改为毛玻璃（磨砂玻璃）视觉：玻璃面板、噪点纹理、柔和描边与阴影、宽圆角的设置卡片、玻璃化搜索框/标签页/提示框 |
| ClickGUI 汉化（`features/module/modules/misc/ModuleClickGuiTranslation.kt`） | 新增 **杂项 → 界面汉化** 模块：开启后 ClickGUI 的分类名、模块名、设置项名称全部显示中文（关闭则显示英文原名）；只影响显示，配置与接口标识不变 |
| 语言文件（`resources/liquidbounce/lang/zh_cn.json`） | 补充 258 个模块名称、8 个分类名称、1222 个设置项名称的中文翻译（新增键，未改动原有条目） |
| 模块列表 HUD（`hud/elements/ArrayList.svelte`） | 重写：阶梯圆角、生长动画、渐变流光文字、4 种样式 / 4 种动画 / 3 种排序 / 侧边线 / 后缀 / 辉光 / 阴影 |
| 目标信息 HUD（`hud/elements/targethud/TargetHud.svelte`） | 重写：Round / Rise / Moon / Simple 四种样式，圆角头像、渐变胶囊血条与掉血残影、掉血弹性回弹、装备格与耐久条、数字或心形血量 |
| 方块计数 HUD（`hud/elements/BlockCounter.svelte`） | 重写：胶囊卡片、弹簧缓动的进度条（满格 64/128/256）、方块数不足时变红告警 |

---

## 运行要求

| 项目 | 要求 |
|---|---|
| Minecraft | 与源码对应的版本（见 `gradle.properties` 中的 `minecraft_version`） |
| 加载器 | Fabric Loader + **Fabric API**、**Fabric Language Kotlin**（两个前置 mod 必须安装） |
| Java | 构建需 JDK 25；运行由启动器按游戏版本提供 |

## 构建

构建需要 [JDK 25](https://adoptium.net/) 与 [Node.js](https://nodejs.org)（用于编译界面主题）。

```bash
# 1. 克隆仓库
git clone https://github.com/Creat319/Veil-Client.git
cd Veil-Client

# 2. 构建（产物在 build/libs/）
./gradlew jar -x generateGitProperties      # Windows: gradlew.bat jar -x generateGitProperties

# 3. 或直接启动客户端调试
./gradlew runClient
```

> `-x generateGitProperties` 用于跳过依赖 git 历史的信息生成任务，在没有上游 git 历史的仓库中需要跳过。

首次构建会自动执行 `npm install` 并编译 `src-theme`（主题界面），编译结果会打包进 jar。

## 开发

- 界面主题位于 `src-theme/`（Svelte + Vite），单独调试可 `cd src-theme && npm run dev`
- 客户端代码位于 `src/main/kotlin/net/ccbluex/liquidbounce/`，REST / WebSocket 接口在 `integration/interop/protocol/`
- 主题组件（HUD 组件的设置项）定义在 `src-theme/public/components/*.json`

## 许可证

本项目（含所有修改）遵循 [GNU General Public License v3.0](LICENSE)，

## 上游信息

**LiquidBounce** — CCBlueX
[Website](https://liquidbounce.net) · [上游源码](https://github.com/CCBlueX/LiquidBounce) · [上游 Issues](https://github.com/CCBlueX/LiquidBounce/issues)

**CCBlueX**
Vahrenwalder Str. 269A
30179 Hanover
Germany
**Owner and responsible for the content:** Marco Beyer

## 免责声明

在多人游戏中使用任何修改客户端都可能违反服务器规则导致封号，**风险自负**。本项目仅用于学习与研究，
作者不对使用本程序造成的任何后果负责。
