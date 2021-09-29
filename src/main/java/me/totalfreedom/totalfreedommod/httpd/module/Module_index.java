package me.totalfreedom.totalfreedommod.httpd.module;

import java.util.Set;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.httpd.HTMLGenerationTools;
import me.totalfreedom.totalfreedommod.httpd.HTTPDPageBuilder;
import me.totalfreedom.totalfreedommod.httpd.NanoHTTPD;
import org.reflections.Reflections;

public class Module_index extends HTTPDModule
{

    public Module_index(NanoHTTPD.HTTPSession session)
    {
        super(session);
    }

    @Override
    public NanoHTTPD.Response getResponse()
    {
        return new HTTPDPageBuilder(body(), title(), null, null).getResponse();
    }

    public String body()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(HTMLGenerationTools.heading("TotalFreedom HTTPd Module List", 1));

        Reflections r = new Reflections("me.totalfreedom.totalfreedommod.httpd.module");

        Set<Class<? extends HTTPDModule>> moduleClasses = r.getSubTypesOf(HTTPDModule.class);

        for (Class<?> c : moduleClasses)
        {
            String name = c.getSimpleName().replace("Module_", "");

            if (name.equals("file"))
            {
                continue;
            }

            // <a href="http://localhost:28966/index">index</a>
            sb.append("<ul><li>");
            sb.append(ConfigEntry.HTTPD_REVERSE_PROXY.getBoolean() ? "<a href=\"https://" : "<a href=\"http://")
                    .append(ConfigEntry.HTTPD_HOST.getString())
                    .append(":")
                    .append(ConfigEntry.HTTPD_REVERSE_PROXY.getBoolean() ? ConfigEntry.HTTPD_REVERSE_PROXY_PORT : ConfigEntry.HTTPD_PORT)
                    .append("/")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a>")
                    .append("</li></ul>");
        }
        return sb.toString();
    }

    public String title()
    {
        return "TotalFreedom :: HTTPd Modules";
    }
}