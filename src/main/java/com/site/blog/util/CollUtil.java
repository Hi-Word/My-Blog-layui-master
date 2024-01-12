package com.site.blog.util;

import java.util.Collection;

public class CollUtil {

    public static boolean isNotEmpty(Collection coll){
        return  coll == null || coll.isEmpty();
    }
}
