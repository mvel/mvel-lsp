# Change Log for client

## 3.0.0-alpha1 (2026-01-21)

### Breaking Changes
- **VSCode**: Minimum version 1.75.0 (was 1.62.0)
- **Node.js**: Minimum version 20.x (was 14.x)
- **Import**: `vscode-languageclient` → `vscode-languageclient/node`

### Dependency Updates

**Major:**
- vscode-languageclient: 5.1.1 → 9.0.1
- TypeScript: 4.4.4 → 5.3.0
- @typescript-eslint: 5.1.0 → 8.0.0
- eslint: 8.1.0 → 9.0.0 (new flat config)
- glob: 7.1.7 → 11.0.0

**Minor:**
- @types/vscode: 1.62.0 → 1.75.0
- @types/node: 14.x → 20.x
- @types/glob: 7.1.4 → 8.1.0
- @types/mocha: 9.0.0 → 10.0.0
- @vscode/test-electron: 1.6.2 → 2.3.0
- @vscode/vsce: 2.19.0 → 3.7.1
- mocha: 9.x → 11.2.2

### Improvements
- Modern LSP protocol support (3.17+)
- Better TypeScript 5.3 type safety
- Improved linting with ESLint 9 flat config
- Enhanced performance and stability
- Security updates

### Migration
- **Users**: Upgrade VSCode to 1.75.0+
- **Developers**: Use Node.js 20.x, update imports if needed
- **Files**: No changes to .mvel files required

## 3.0.0-alpha1 (Development Build)

- This is an alpha SNAPSHOT release, so the latest alpha will include all the latest changes.