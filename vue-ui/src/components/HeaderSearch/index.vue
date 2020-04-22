<template>
  <!--头部的路由搜索-->
  <div :class="{'show':show}" class="header-search">
    <svg-icon class-name="search-icon" icon-class="search" @click.stop="click"/>
    <el-select
      ref="headerSearchSelect"
      v-model="search"
      :remote-method="querySearch"
      filterable
      default-first-option
      remote
      placeholder="Search"
      class="header-search-select"
      @change="change"
    >
      <el-option v-for="item in options" :key="item.path" :value="item" :label="item.title.join(' > ')"/>
    </el-select>
  </div>
</template>

<script>
  // fuse是一个轻量级的模糊搜索模块
  // 使搜索结果更符合期望
  import Fuse from 'fuse.js'
  import path from 'path'

  export default {
    name: 'HeaderSearch',
    data() {
      return {
        search: '',
        options: [],
        searchPool: [],
        show: false,
        fuse: undefined
      }
    },
    computed: {
      // 获取所有路由
      routes() {
        return this.$store.getters.permission_routes
      }
    },
    watch: {
      routes() {
        this.searchPool = this.generateRoutes(this.routes)
      },
      searchPool(list) {
        this.initFuse(list)
      },
      show(value) {
        // 如果show为true,添加一个close事件，如果为false，取消这个事件。
        // 当用户点击body任何地方，触发close 隐藏搜索框，失去焦点，清空数据。
        if (value) {
          document.body.addEventListener('click', this.close)
        } else {
          document.body.removeEventListener('click', this.close)
        }
      }
    },
    mounted() {
      this.searchPool = this.generateRoutes(this.routes)
    },
    methods: {
      // 点击放大镜后方法
      click() {
        this.show = !this.show
        if (this.show) {
          //  HTMLElement.focus()方法可以设置指定元素获取焦点。
          // && 这种写法其实相当于，&& 如果前面的结果是 false，就不运行后面的了。
          this.$refs.headerSearchSelect && this.$refs.headerSearchSelect.focus()
        }
      },
      close() {
        // 失去焦点
        this.$refs.headerSearchSelect && this.$refs.headerSearchSelect.blur()
        this.options = []
        this.show = false
      },
      change(val) {
        this.$router.push(val.path)
        this.search = ''
        this.options = []
        this.$nextTick(() => {
          this.show = false
        })
      },
      initFuse(list) {
        this.fuse = new Fuse(list, {
          shouldSort: true,
          threshold: 0.4,
          location: 0,
          distance: 100,
          maxPatternLength: 32,
          minMatchCharLength: 1,
          keys: [{
            name: 'title',
            weight: 0.7
          }, {
            name: 'path',
            weight: 0.3
          }]
        })
      },
      // Filter out the routes that can be displayed in the sidebar
      // And generate the internationalized title
      generateRoutes(routes, basePath = '/', prefixTitle = []) {
        let res = []
        for (const router of routes) {
          // skip hidden router
          if (router.hidden) {
            continue
          }

          const data = {
            path: path.resolve(basePath, router.path),
            title: [...prefixTitle]
          }

          if (router.meta && router.meta.title) {
            data.title = [...data.title, router.meta.title]

            if (router.redirect !== 'noRedirect') {
              // only push the routes with title
              // special case: need to exclude parent router without redirect
              res.push(data)
            }
          }

          // recursive child routes
          if (router.children) {
            const tempRoutes = this.generateRoutes(router.children, data.path, data.title)
            if (tempRoutes.length >= 1) {
              res = [...res, ...tempRoutes]
            }
          }
        }
        return res
      },
      querySearch(query) {
        if (query !== '') {
          this.options = this.fuse.search(query)
        } else {
          this.options = []
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .header-search {
    font-size: 0 !important;

    .search-icon {
      cursor: pointer;
      font-size: 18px;
      vertical-align: middle;
    }

    .header-search-select {
      font-size: 18px;
      transition: width 0.2s;
      width: 0;
      overflow: hidden;
      background: transparent;
      border-radius: 0;
      display: inline-block;
      vertical-align: middle;

      /deep/ .el-input__inner {
        border-radius: 0;
        border: 0;
        padding-left: 0;
        padding-right: 0;
        box-shadow: none !important;
        border-bottom: 1px solid #d9d9d9;
        vertical-align: middle;
      }
    }

    &.show {
      .header-search-select {
        width: 210px;
        margin-left: 10px;
      }
    }
  }
</style>
