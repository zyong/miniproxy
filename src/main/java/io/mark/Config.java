package io.mark;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;

public class Config {
    static String CONFIGFILE = "conf/application.yaml";
    private final LinkedHashMap configs;

    public Config() {
        Yaml ymlObj = new Yaml();
        InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(CONFIGFILE);
        configs = ymlObj.loadAs(inputStream, LinkedHashMap.class);
        ymlObj = null;
    }

    public Object get(String key) throws Exception {
        String[] keys;
        if (key.contains("/")) {
            keys = key.split("/");
        } else {
            keys = new String[]{key};
        }

        LinkedHashMap elem = null;
        if (configs.containsKey(keys[0]) && configs.get(keys[0]).getClass() == LinkedHashMap.class) {
            elem = (LinkedHashMap) configs.get(keys[0]);
        } else {
            return configs.get(keys[0]);
        }

        if (keys.length == 1) {
            return elem;
        }

        for (int i=1; i<keys.length; i++) {
            String k = keys[i];
            if (elem.containsKey(k) &&
                    elem.get(k).getClass() == LinkedHashMap.class) {
                        elem = (LinkedHashMap) configs.get(k);
            } else {
                if (elem.containsKey(k)) {
                    return elem.get(k);
                } else {
                    throw new Exception("key not exists");
                }
            }
        }
        return elem;
    }
}
