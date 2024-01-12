package com.site.blog.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @program: my-blog
 * @classname: UploadFileUtils
 * @description: 上传文件工具类
 * @author: linn
 * @create: 2019-08-24 15:24
 **/
public class UploadFileUtils {

    /**
     * @Description: 获取图片后缀
     * @Param: [file]
     * @return: java.lang.String
     * @date: 2019/8/24 15:27
     */
    public static String getSuffixName(MultipartFile file){
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)){
            throw new RuntimeException("获取图片后缀失败");
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    /**
     * @Description: 生成文件名称通用方法
     * @Param: [suffixName] 图片后缀
     * @return: java.lang.String
     * @date: 2019/8/24 15:29
     */
    public static String getNewFileName(String suffixName){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        int random = new Random().nextInt(100);
        StringBuilder tempName = new StringBuilder();
        tempName.append(sdf.format(new Date())).append(random).append(suffixName);
        return tempName.toString();
    }


    /**
     * 将pdf文档转换成字节数组
     *
     * @return 返回对应PDF文档的字节数组
     */
    public static byte[] getPdfBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len;
        while ((len = inputStream.read(data)) != -1) {
            out.write(data, 0, len);
        }
        return out.toByteArray();
    }

    /**
     * 基于内存中的字节数组进行PDF文档的合并
     *
     * @param firstPdf  第一个PDF文档
     * @param secondPdf 第二个PDF文档
     */
    public static byte[] mergePdfBytes(byte[] firstPdf, byte[] secondPdf) {
        try {
            if (firstPdf != null && secondPdf != null) {
                // 创建字节数组，基于内存进行合并
                ByteArrayOutputStream bass = new ByteArrayOutputStream();
                PdfDocument destDoc = new PdfDocument(new PdfWriter(bass));
                // 合并的pdf文件对象
                PdfDocument firstDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(firstPdf)));
                PdfDocument secondDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(secondPdf)));
                // 合并对象
                PdfMerger merger = new PdfMerger(destDoc);
                merger.merge(firstDoc, 1, firstDoc.getNumberOfPages());
                merger.merge(secondDoc, 1, secondDoc.getNumberOfPages());
                // 关闭文档流
                merger.close();
                firstDoc.close();
                secondDoc.close();
                destDoc.close();
                return bass.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        return null;
    }

}
