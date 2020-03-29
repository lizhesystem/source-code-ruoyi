// 用于idea查找别名-跟项目代码无关
// 在webpack里配置该文件
function resolve(dir) {
  return path.join(__dirname, dir)
}

module.exports = {
  resolve: {
    alias: {
      '@': resolve('src')
    }
  }
}
