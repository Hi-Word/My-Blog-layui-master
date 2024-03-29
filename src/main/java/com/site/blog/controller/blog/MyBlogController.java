package com.site.blog.controller.blog;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.site.blog.constants.*;
import com.site.blog.entity.*;
import com.site.blog.pojo.dto.AjaxPutPage;
import com.site.blog.pojo.dto.AjaxResultPage;
import com.site.blog.pojo.dto.BlogPageCondition;
import com.site.blog.pojo.dto.Result;
import com.site.blog.pojo.vo.BlogDetailVO;
import com.site.blog.service.*;
import com.site.blog.util.CollUtil;
import com.site.blog.util.PageResult;
import com.site.blog.util.ResultGenerator;
import com.site.blog.util.UploadFileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 博客前台
 *
 * @author Linn-cn
 * @date 2020/12/7
 */
@Controller
public class MyBlogController {

    public static String theme = "amaze";

    @Autowired
    private BlogInfoService blogInfoService;

    @Autowired
    private BlogTagService blogTagService;

    @Autowired
    private BlogConfigService blogConfigService;

    @Autowired
    private BlogTagRelationService blogTagRelationService;

    @Autowired
    private BlogCommentService blogCommentService;

    @Autowired
    private BlogLinkService blogLinkService;

    /**
     * 博客首页
     * @param request
     * @author Linn-cn
     * @date 2020/12/7
     */
    @GetMapping({"/", "/index", "index.html"})
    public String index(HttpServletRequest request) {
        return this.page(request, new BlogPageCondition()
                .setPageNum(1)
                .setPageName("首页")
        );
    }

    /**
     * 分类
     *
     * @param request
     * @param categoryName
     * @author Linn-cn
     * @date 2020/12/7
     */
    @GetMapping({"/category/{categoryName}"})
    public String category(HttpServletRequest request, @PathVariable("categoryName") String categoryName) {
        return this.page(request, new BlogPageCondition()
                .setPageNum(1)
                .setPageName("分类")
                .setCategoryName(categoryName));
    }

    /**
     * 搜索
     *
     * @param request
     * @param keyword
     * @return java.lang.String
     * @date 2019/9/6 7:03
     */
    @GetMapping({"/search/{keyword}"})
    public String search(HttpServletRequest request, @PathVariable("keyword") String keyword) {
        return this.page(request, new BlogPageCondition()
                .setPageNum(1)
                .setPageName("首页")
                .setKeyword(keyword)
        );
    }

    /**
     * 标签
     *
     * @param request
     * @param tagId
     * @return java.lang.String
     * @date 2019/9/6 7:04
     */
    @GetMapping({"/tag/{tagId}"})
    public String tag(HttpServletRequest request, @PathVariable("tagId") String tagId) {
        return this.page(request, new BlogPageCondition()
                .setPageNum(1)
                .setPageName("标签")
                .setTagId(tagId));
    }

    /**
     * 博客分页
     *
     * @param request
     * @param condition
     * @throws
     * @author Linn-cn
     * @date 2020/12/7
     */
    @GetMapping({"/page"})
    public String page(HttpServletRequest request, BlogPageCondition condition) {
        if (Objects.isNull(condition.getPageNum())) {
            condition.setPageNum(1);
        }
        if (Objects.isNull(condition.getPageSize())) {
            condition.setPageSize(5);
        }
        Page<BlogInfo> page = new Page<>(condition.getPageNum(), condition.getPageSize());
        LambdaQueryWrapper<BlogInfo> sqlWrapper = Wrappers.<BlogInfo>lambdaQuery()
                .like(Objects.nonNull(condition.getKeyword()), BlogInfo::getBlogTitle, condition.getKeyword())
                .eq(Objects.nonNull(condition.getCategoryName()), BlogInfo::getBlogCategoryName, condition.getCategoryName())
                .eq(BlogInfo::getBlogStatus, BlogStatusEnum.RELEASE.getStatus())
                .eq(BlogInfo::getIsDeleted, DeleteStatusEnum.NO_DELETED.getStatus());
        if (Objects.nonNull(condition.getTagId())) {
            List<BlogTagRelation> list = blogTagRelationService.list(new QueryWrapper<BlogTagRelation>()
                    .lambda().eq(BlogTagRelation::getTagId, condition.getTagId()));
            if (!CollectionUtils.isEmpty(list)) {
                sqlWrapper.in(BlogInfo::getBlogId, list.stream().map(BlogTagRelation::getBlogId).toArray());
            }
        }
        sqlWrapper.orderByDesc(BlogInfo::getCreateTime);
        blogInfoService.page(page, sqlWrapper);
        PageResult blogPageResult = new PageResult(page.getRecords(), page.getTotal(), condition.getPageSize(), condition.getPageNum());
        if (Objects.nonNull(condition.getKeyword())) {
            request.setAttribute("keyword", condition.getKeyword());
        }
        if (Objects.nonNull(condition.getTagId())) {
            request.setAttribute("tagId", condition.getTagId());
        }
        if (Objects.nonNull(condition.getCategoryName())) {
            request.setAttribute("categoryName", condition.getCategoryName());
        }
        request.setAttribute("blogPageResult", blogPageResult);
        request.setAttribute("pageName", condition.getPageName());
        request.setAttribute("newBlogs", blogInfoService.getNewBlog());
        request.setAttribute("hotBlogs", blogInfoService.getHotBlog());
        request.setAttribute("hotTags", blogTagService.getBlogTagCountForIndex());
        request.setAttribute("configurations", blogConfigService.getAllConfigs());
        return "blog/" + theme + "/index";
    }

    /**
     * 文章详情
     *
     * @param request
     * @param blogId
     * @return java.lang.String
     * @date 2019/9/6 13:09
     */
    @GetMapping({"/blog/{blogId}", "/article/{blogId}"})
    public String detail(HttpServletRequest request, @PathVariable("blogId") Long blogId) {
        // 获得文章info
        BlogInfo blogInfo = blogInfoService.getById(blogId);
        List<BlogTagRelation> blogTagRelations = blogTagRelationService.list(new QueryWrapper<BlogTagRelation>()
                .lambda()
                .eq(BlogTagRelation::getBlogId, blogId));
        blogInfoService.updateById(new BlogInfo()
                .setBlogId(blogInfo.getBlogId())
                .setBlogViews(blogInfo.getBlogViews() + 1));

        // 获得关联的标签列表
        List<Integer> tagIds;
        List<BlogTag> tagList = new ArrayList<>();
        if (!blogTagRelations.isEmpty()) {
            tagIds = blogTagRelations.stream()
                    .map(BlogTagRelation::getTagId).collect(Collectors.toList());
            tagList = blogTagService.list(new QueryWrapper<BlogTag>().lambda().in(BlogTag::getTagId, tagIds));
        }

        // 关联评论的Count
        Integer blogCommentCount = blogCommentService.count(new QueryWrapper<BlogComment>()
                .lambda()
                .eq(BlogComment::getCommentStatus, CommentStatusEnum.ALLOW.getStatus())
                .eq(BlogComment::getIsDeleted, DeleteStatusEnum.NO_DELETED.getStatus())
                .eq(BlogComment::getBlogId, blogId));

        BlogDetailVO blogDetailVO = new BlogDetailVO();
        BeanUtils.copyProperties(blogInfo, blogDetailVO);
        blogDetailVO.setCommentCount(blogCommentCount);
        request.setAttribute("blogDetailVO", blogDetailVO);
        request.setAttribute("tagList", tagList);
        request.setAttribute("pageName", "详情");
        request.setAttribute("configurations", blogConfigService.getAllConfigs());
        return "blog/" + theme + "/detail";
    }

    /**
     * 评论列表
     *
     * @param ajaxPutPage
     * @param blogId
     * @return com.site.blog.pojo.dto.AjaxResultPage<com.site.blog.entity.BlogComment>
     * @date 2019/11/19 8:42
     */
    @GetMapping("/blog/listComment")
    @ResponseBody
    public AjaxResultPage<BlogComment> listComment(AjaxPutPage<BlogComment> ajaxPutPage, Integer blogId) {
        Page<BlogComment> page = ajaxPutPage.putPageToPage();
        blogCommentService.page(page, new QueryWrapper<BlogComment>()
                .lambda()
                .eq(BlogComment::getBlogId, blogId)
                .eq(BlogComment::getCommentStatus, CommentStatusEnum.ALLOW.getStatus())
                .eq(BlogComment::getIsDeleted, DeleteStatusEnum.NO_DELETED.getStatus())
                .orderByDesc(BlogComment::getCommentCreateTime));
        AjaxResultPage<BlogComment> ajaxResultPage = new AjaxResultPage<>();
        ajaxResultPage.setCount(page.getTotal());
        ajaxResultPage.setData(page.getRecords());
        return ajaxResultPage;
    }

    /**
     * 友链界面
     *
     * @param request
     * @return java.lang.String
     * @date 2019/9/6 17:26
     */
    @GetMapping({"/link"})
    public String link(HttpServletRequest request) {
        request.setAttribute("pageName", "友情链接");
        List<BlogLink> favoriteLinks = blogLinkService.list(new QueryWrapper<BlogLink>()
                .lambda().eq(BlogLink::getLinkType, LinkConstants.LINK_TYPE_FRIENDSHIP.getLinkTypeId())
        );
        List<BlogLink> recommendLinks = blogLinkService.list(new QueryWrapper<BlogLink>()
                .lambda().eq(BlogLink::getLinkType, LinkConstants.LINK_TYPE_RECOMMEND.getLinkTypeId())
        );
        List<BlogLink> personalLinks = blogLinkService.list(new QueryWrapper<BlogLink>()
                .lambda().eq(BlogLink::getLinkType, LinkConstants.LINK_TYPE_PRIVATE.getLinkTypeId())
        );
        //判断友链类别并封装数据 0-友链 1-推荐 2-个人网站
        request.setAttribute("favoriteLinks", favoriteLinks);
        request.setAttribute("recommendLinks", recommendLinks);
        request.setAttribute("personalLinks", personalLinks);
        request.setAttribute("configurations", blogConfigService.getAllConfigs());
        return "blog/" + theme + "/link";
    }

    /**
     * 代码墙
     *
     * @return 视图名
     */
    @GetMapping("/test")
    public String test() {
        return "blog/" + theme + "/test";  // 返回test.html，框架会根据配置找到对应的视图页面
    }

    /**
     * 五子棋
     *
     * @return 视图名
     */
    @GetMapping("/gobang")
    public String  gobang() {
        return "blog/" + theme + "/gobang";  // 返回test.html，框架会根据配置找到对应的视图页面
    }

    /**
     * 提交评论
     *
     * @return com.site.blog.pojo.dto.Result
     * @date 2019/9/6 17:40
     */
    @PostMapping(value = "/blog/comment")
    @ResponseBody
    public Result<String> comment(HttpServletRequest request,
                                  @Validated BlogComment blogComment) {
        String ref = request.getHeader("Referer");
        // 对非法字符进行转义，防止xss漏洞
        blogComment.setCommentBody(StringEscapeUtils.escapeHtml4(blogComment.getCommentBody()));
        if (StringUtils.isEmpty(ref)) {
            return ResultGenerator.getResultByHttp(HttpStatusEnum.INTERNAL_SERVER_ERROR, "非法请求");
        }
        boolean flag = blogCommentService.save(blogComment);
        if (flag) {
            return ResultGenerator.getResultByHttp(HttpStatusEnum.OK);
        }
        return ResultGenerator.getResultByHttp(HttpStatusEnum.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/merge")
    public void mergePdf(@RequestParam("file1") MultipartFile file1,
                         @RequestParam("file2") MultipartFile file2,
                         @RequestParam("fileName") String fileName,
                         HttpServletResponse response) {
        try {
            // 获取输入流列表
            List<InputStream> inputStreamList = getInputStreams(file1, file2);

            // 合并PDF并获取合并后的数据
            byte[] mergedPdfData = mergePdf(inputStreamList);

            // 返回PDF数据，传入文件名
            sendMergedPdfResponse(response, mergedPdfData, fileName);
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            // 在实际应用中，你可以返回适当的错误响应，比如 500 Internal Server Error
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    // 获取输入流列表的方法
    private List<InputStream> getInputStreams(MultipartFile file1, MultipartFile file2) {
        List<InputStream> inputStreamList = new ArrayList<>();

        try {
            // 使用 MultipartFile 获取文件的 InputStream
            InputStream inputStream1 = file1.getInputStream();
            InputStream inputStream2 = file2.getInputStream();
            inputStreamList.add(inputStream1);
            inputStreamList.add(inputStream2);
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
        }

        return inputStreamList;
    }

    // 合并PDF的方法
    private byte[] mergePdf(List<InputStream> inputStreamList) throws IOException {
        // 实现PDF合并逻辑，使用你之前提到的 UploadFileUtils.mergePdfBytes 方法
        // 这里假设该方法返回合并后的PDF的字节数组

        byte[] mergedPdfData = null;

        int size = inputStreamList.size();
        if (size > 0) {
            mergedPdfData = StreamUtils.copyToByteArray(inputStreamList.get(0));
            for (int i = 1; i < size; i++) {
                InputStream currentInputStream = inputStreamList.get(i);
                if (currentInputStream != null) {
                    byte[] pdfBytes = StreamUtils.copyToByteArray(currentInputStream);
                    mergedPdfData = UploadFileUtils.mergePdfBytes(mergedPdfData, pdfBytes);
                }
            }
        }

        return mergedPdfData;
    }

    // 修改 sendMergedPdfResponse 方法，接收文件名参数
    private void sendMergedPdfResponse(HttpServletResponse response, byte[] mergedPdfData, String fileName) throws IOException {
        if (mergedPdfData != null) {
            response.setContentType("application/pdf");
            // 使用用户指定的文件名
            response.setHeader("Content-Disposition", "inline; filename=" + fileName);

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(mergedPdfData);
                outputStream.flush();
            }
        }
    }
}
