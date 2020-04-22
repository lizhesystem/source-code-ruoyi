<template>
  <!--头部的布局大小-->
  <el-dropdown trigger="click" @command="handleSetSize">
    <div>
      <svg-icon class-name="size-icon" icon-class="size"/>
    </div>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item v-for="item of sizeOptions" :key="item.value" :disabled="size===item.value"
                        :command="item.value">
        {{ item.label }}
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
  export default {
    data() {
      return {
        sizeOptions: [
          { label: 'Default', value: 'default' },
          { label: 'Medium', value: 'medium' },
          { label: 'Small', value: 'small' },
          { label: 'Mini', value: 'mini' }
        ]
      }
    },
    computed: {
      size() {
        return this.$store.getters.size
      }
    },
    methods: {
      // https://element.eleme.cn/#/zh-CN/component/quickstart#quan-ju-pei-zhi
      // 在引入 Element 时，可以传入一个全局配置对象。该对象目前支持 size 与 zIndex 字段。size 用于改变组件的默认尺寸，zIndex 设置弹框的初始 z-index（默认值：2000）
      handleSetSize(size) {
        // 修改字体大小
        this.$ELEMENT.size = size
        // 设置到vuex里和cookie里 Cookies.set('size', size)
        this.$store.dispatch('app/setSize', size)
        this.refreshView()
        this.$message({
          message: 'Switch Size Success',
          type: 'success'
        })
      },
      refreshView() {
        // In order to make the cached page re-rendered 使面包屑缓存的页面重新呈现
        this.$store.dispatch('tagsView/delAllCachedViews', this.$route)
        const { fullPath } = this.$route
        // console.log(fullPath)  当前的路由
        // Vue.nextTick()作用：在下次dom更新循环结束之后执行延迟回调。在修改数据之后立即使用这个方法，获得更新后的dom
        // 在数据变化后要执行的某个操作，而这个操作需要使用随数据改变而改变的DOM结构的时候，这个操作都应该放进Vue.nextTick()的回调函数中。
        this.$nextTick(() => {
          // 确保dom已经更新,字体已经改变
          this.$router.replace({
            path: '/redirect' + fullPath
          })
        })
      }
    }
  }
</script>
