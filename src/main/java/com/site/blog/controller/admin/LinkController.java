package com.site.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.site.blog.constants.HttpStatusEnum;
import com.site.blog.constants.LinkConstants;
import com.site.blog.pojo.dto.AjaxPutPage;
import com.site.blog.pojo.dto.AjaxResultPage;
import com.site.blog.pojo.dto.Result;
import com.site.blog.entity.BlogLink;
import com.site.blog.service.BlogLinkService;
import com.site.blog.util.DateUtils;
import com.site.blog.util.ResultGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @qq交流群 796794009
 * @qq 1320291471
 * @Description: 友链Controller
 * @create 2020/12/27
 */
@Controller
@RequestMapping("/admin")
public class LinkController {

    @Autowired
    private BlogLinkService blogLinkService;

    @GetMapping("/v1/linkType")
    public String gotoLink(){
        return "adminLayui/link-list";
    }

    @ResponseBody
    @GetMapping("/v1/linkType/list")
    public Result<List<BlogLink>> linkTypeList(){
        List<BlogLink> links = new ArrayList<>();
        links.add(new BlogLink().setLinkType(LinkConstants.LINK_TYPE_FRIENDSHIP.getLinkTypeId())
                .setLinkName(LinkConstants.LINK_TYPE_FRIENDSHIP.getLinkTypeName()));
        links.add(new BlogLink().setLinkType(LinkConstants.LINK_TYPE_RECOMMEND.getLinkTypeId())
                .setLinkName(LinkConstants.LINK_TYPE_RECOMMEND.getLinkTypeName()));
        links.add(new BlogLink().setLinkType(LinkConstants.LINK_TYPE_PRIVATE.getLinkTypeId())
                .setLinkName(LinkConstants.LINK_TYPE_PRIVATE.getLinkTypeName()));
        return ResultGenerator.getResultByHttp(HttpStatusEnum.OK,links);
    }

    @ResponseBody
    @GetMapping("/v1/link/paging")
    public AjaxResultPage<BlogLink> getLinkList(AjaxPutPage<BlogLink> ajaxPutPage, BlogLink condition){
        QueryWrapper<BlogLink> queryWrapper = new QueryWrapper<>(condition);
        queryWrapper.lambda()
                .orderByAsc(BlogLink::getLinkRank);
        Page<BlogLink> page = ajaxPutPage.putPageToPage();
        blogLinkService.page(page,queryWrapper);
        AjaxResultPage<BlogLink> result = new AjaxResultPage<>();
        result.setData(page.getRecords());
        result.setCount(page.getTotal());
        return result;
    }

    @ResponseBody
    @PostMapping("/v1/link/isDel")
    public Result<String> updateLinkStatus(BlogLink blogLink){
        boolean flag = blogLinkService.updateById(blogLink);
        if (flag){
            return ResultGenerator.getResultByHttp(HttpStatusEnum.OK);
        }
        return ResultGenerator.getResultByHttp(HttpStatusEnum.INTERNAL_SERVER_ERROR);
    }

    @ResponseBody
    @PostMapping("/v1/link/clear")
    public Result<String> clearLink(Integer linkId){
        boolean flag = blogLinkService.removeById(linkId);
        if (flag){
            return ResultGenerator.getResultByHttp(HttpStatusEnum.OK);
        }
        return ResultGenerator.getResultByHttp(HttpStatusEnum.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/v1/link/edit")
    public String editLink(Integer linkId, Model model){
        if (linkId != null){
            BlogLink blogLink = blogLinkService.getById(linkId);
            model.addAttribute("blogLink",blogLink);
        }
        return "adminLayui/link-edit";
    }

    @ResponseBody
    @PostMapping("/v1/link/edit")
    public Result<String> updateAndSaveLink(BlogLink blogLink){
        blogLink.setCreateTime(DateUtils.getLocalCurrentDate());
        boolean flag;
        if (blogLink.getLinkId() != null){
            flag = blogLinkService.updateById(blogLink);
        }else{
            flag = blogLinkService.save(blogLink);
        }
        if (flag){
            return ResultGenerator.getResultByHttp(HttpStatusEnum.OK);
        }
        return ResultGenerator.getResultByHttp(HttpStatusEnum.INTERNAL_SERVER_ERROR);
    }
}
