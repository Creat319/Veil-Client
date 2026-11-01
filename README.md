<div align="center">

# Veil Client

基于 [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) 二次修改的 Fabric 客户端（Minecraft 客户端修改）。
界面、HUD 组件经过重新设计，保留 LiquidBounce 的全部功能与配置体系。

</div>

---

## ⚠️ 二次分发声明（请先阅读）

**本项目是 LiquidBounce 的二次分发（fork / 衍生作品），不是官方版本。**

- 上游项目：**LiquidBounce** — <https://github.com/CCBlueX/LiquidBounce>
- 上游作者：**CCBlueX**（Marco Beyer 及贡献者）
- 上游许可证：**GNU General Public License v3.0**
- 本仓库与 CCBlueX 官方**没有任何关系**，官方不对本仓库的任何内容负责；本仓库的问题请不要提交到上游。
- 本仓库的修改部分与上游一样，**同样以 GPL-3.0 分发**，完整源代码即在本仓库中公开（符合 GPL-3.0 对修改作品必须开源的要求）。
- 原版权声明、许可证文件（[LICENSE](LICENSE)）与第三方声明（[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)）均已原样保留。
  代码中的包名、模块名、配置键（`liquidbounce.*`）、fabric mod id 等**内部标识保持与上游一致**，以便与上游代码和配置兼容；
  对外展示的项目名称使用 **Veil Client**。

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

> 修改日期：2025 年（详见仓库提交记录）。

---

## 功能一览

- **ClickGUI**：毛玻璃界面，右键模块展开设置，支持搜索（可搜中文）、拖拽面板、窗口模式
- **界面汉化**：杂项分类里的「界面汉化」模块，一键中英切换，即时生效
- **HUD 组件**：模块列表、目标信息、方块计数、TabGui、Notification、Keystrokes 等，可在 HUD 编辑器中自由摆放、缩放、单独配置样式
- **其余功能**：战斗 / 移动 / 玩家 / 渲染 / 世界 / 杂项 / 漏洞 / 娱乐 八大类模块，配置体系、脚本（ScriptAPI）、主题系统等均沿用上游

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

本项目（含所有修改）遵循 [GNU General Public License v3.0](LICENSE)，与上游一致。许可证只覆盖本仓库中直接包含的源代码；
开发与编译过程中可能使用到的其他来源代码不在其覆盖范围内。

*你可以：* 使用、分享、修改。

*如果你确实使用了本项目的任何代码：*

- **必须公开你的修改作品的源代码，以及取自本项目的源代码部分；不允许把本项目代码（哪怕是部分）用于闭源（或混淆）程序。**
- **你的修改作品同样必须以 GPL 许可证分发。**

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
