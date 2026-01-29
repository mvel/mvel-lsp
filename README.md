## mvel-lsp

Language Server Protocol (LSP) implementation for [MVEL3](https://github.com/mvel/mvel) (MVFLEX Expression Language 3), providing code completion support in VS Code.

### Features

- Keyword completion
- Type completion (imported classes)
- Method and field completion after `.` access
- Property access syntax sugar (e.g. `name` for `getName()`/`setName()`)
- Inline cast support (`expression#ClassName#`)

### Requirements

- Java 17 or later

### Installation

1. Download the `.vsix` file from the [GitHub Releases](https://github.com/tkobayas/mvel-lsp/releases) page
2. In VS Code, open the Command Palette (`Ctrl+Shift+P`) and run `Extensions: Install from VSIX...`
3. Select the downloaded `.vsix` file

### Building from Source

```bash
mvn clean package
cd client
npm install
npm run pack:dev    # generates .vsix in client/dist/
```

### Components

- `mvel3-completion` – Code completion engine for MVEL3
- `mvel3-lsp-server` – LSP server that exposes the code completion functionality
- `client` – VS Code extension `vscode-extension-mvel3-editor`

See [Developer_Notes.md](Developer_Notes.md) for development and debugging instructions.

### License

[Apache License 2.0](LICENSE)
