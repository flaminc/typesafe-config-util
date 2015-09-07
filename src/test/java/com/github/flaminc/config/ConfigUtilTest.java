package com.github.flaminc.config;

import com.github.flaminc.config.test.BeanWithMap;
import com.github.flaminc.config.test.EnumBean;
import com.github.flaminc.config.test.EnumInstance;
import com.github.flaminc.config.test.SimpleBean;
import com.github.flaminc.config.test.TestBean;
import com.github.flaminc.config.test.inheritance.Child;
import com.github.flaminc.config.test.inheritance.ParentBean;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigUtilTest {

    @Test
    public void testConstructTypeNullInvalid() throws Exception {
        final String cStr = "{~constructor:[1]}";
        final Config config = ConfigFactory.parseString(cStr);

        try {
            ConfiguratorUtil.noConfig().construct(config, null);
        } catch (ConfigException.Missing e) {
            assertThat(e).hasMessageContaining("No configuration setting found for key '\"~type\"'");
        }
    }

    @Test
    public void testConstructTypeNullValid() throws Exception {
        final String cStr = "{~constructor:[blah],~type:java.lang.String}";
        final Config config = ConfigFactory.parseString(cStr);

        final String val = ConfiguratorUtil.noConfig().construct(config, null);
        assertThat(val).isEqualTo("blah");

    }

    @Test
    public void testConstructStringFromInt() throws Exception {
        final String cStr = "{~constructor:[\"1\"],~type:java.lang.String}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final String val = ConfiguratorUtil.noConfig().construct(config, null);
        assertThat(val).isEqualTo("1");

    }

    @Test
    public void testLoadClassOfBooleanObject() throws Exception {
        final String cStr = "{objBoolean:true}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final TestBean bean = ConfiguratorUtil.withConfig(config).loadClass(new TestBean());
        assertThat(bean.getObjBoolean()).isEqualTo(true);
    }

    @Test
    public void testLoadClassOfBooleanPrimitive() throws Exception {
        final String cStr = "{primBoolean:true}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final TestBean bean = ConfiguratorUtil.withConfig(config).loadClass(new TestBean());
        assertThat(bean.isPrimBoolean()).isEqualTo(true);
    }

    @Test
    public void testConstructStringFromLeadingZero() throws Exception {
        final String cStr = "{~constructor:[\"01\"],~type:java.lang.String}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final String val = ConfiguratorUtil.noConfig().construct(config, null);
        assertThat(val).isEqualTo("01");
    }

    @Test
    public void testConstructStringFromLeadingZero2() throws Exception {
        final String cStr = "{~constructor:[01],~type:java.lang.String}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists
        try {
            ConfiguratorUtil.noConfig().construct(config, null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Cannot find constructor of types: [class java.lang.Integer] " +
                    "for class java.lang.String");
        }
    }

    @Test
    public void testConstructStringFromFloat() throws Exception {
        final String cStr = "{~constructor:[\"1.01\"],~type:java.lang.String}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final String val = ConfiguratorUtil.noConfig().construct(config, null);
        assertThat(val).isEqualTo("1.01");
    }

    @Test
    public void testConstructWithoutConstruct() throws Exception {
        final String cStr = "{value:blah}";
        final Config config = ConfigFactory.parseString(cStr);

        // config loads the 1 as an integer but in this context we wanted to call the string constructor as no int
        // constructor exists

        final SimpleBean val = ConfiguratorUtil.withConfig(config).construct(SimpleBean.class);

        final SimpleBean expected = new SimpleBean();
        expected.setValue("blah");
        assertThat(val).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testConstructPrimitive() throws Exception {
        final String cStr = "{~constructor:[101],~type:java.lang.Integer}";
        final Config config = ConfigFactory.parseString(cStr);

        final Integer val = ConfiguratorUtil.withConfig(config).construct(null);
        assertThat(val).isEqualTo(new Integer(101));
    }

    @Test
    public void testConstructFromRef() throws Exception {

        final String bStr = "{var.ptr:blah}";
        final Config baseCfg = ConfigFactory.parseString(bStr);
        final String cStr = "{~constructor:[{~ref:var.ptr,~type:java.lang.String}],~type:java.lang.String}";
        final Config cfg = ConfigFactory.parseString(cStr);

        final String val = ConfiguratorUtil.withConfig(baseCfg).construct(cfg, null);
        assertThat(val).isEqualTo("blah");
    }


    @Test
    public void testReference() throws Exception {
        final String cStr = "var.blah:indirect,var.obj:var.blah,var:{ptr1.~ref=var.obj,ptr2.~ref=var.obj,ptr3.~ref=${var.obj}}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final Config var = config.getConfig("var");
        final String ptr1 = util.getObject(var, "ptr1", String.class);
        final String ptr2 = util.getObject(var, "ptr2", String.class);
        final String ptr3 = util.getObject(var, "ptr3", String.class);

        assertThat(ptr1).isSameAs(ptr2).isEqualTo("var.blah");
        assertThat(ptr3).isEqualTo("indirect");
    }

    @Test
    public void testReferenceShort() throws Exception {
        // this should product an error as the reference key has no namespace, like var. for normal variables
        String ref = "blah";

        final String cStr = "blah:blah,var:{ptr1.~ref=" + ref + "}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final Config var = config.getConfig("var");
        try {
            util.getObject(var, "ptr1", String.class);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e).isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Expected path to have at least 2 parts in [" + ref + "]");
        }
    }

    @Test
    public void testReferenceNoHandler() throws Exception {
        // there is no handler for blah
        final String cStr = "blah.ptr:blah,var:{ptr1.~ref=blah.ptr}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final Config var = config.getConfig("var");
        try {
            util.getObject(var, "ptr1", String.class);
            Assert.fail("expected exception");
        } catch (RuntimeException e) {
            assertThat(e).isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot find reference handler named: 'blah'");
        }
    }

    @Test
    public void testReferenceHandlerTypeInfer() throws Exception {
        // the type should be inferred and passed to the handler
        final String cStr = "{value:{~ref=blah.ptr}}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final ReferenceHandler handler = mock(ReferenceHandler.class);
        when(handler.resolve(eq("blah.ptr"), eq("ptr"), same(config.getConfig("value")), eq(String.class)))
                .thenReturn("heyo");
        util.addHandler("blah", handler);
        final SimpleBean bean = util.loadClass(new SimpleBean());
        final SimpleBean expect = new SimpleBean("heyo");
        verify(handler, times(1)).resolve(eq("blah.ptr"), eq("ptr"), same(config.getConfig("value")), eq(String.class));
        assertThat(bean).isEqualToComparingFieldByField(expect);
    }

    @Test
    public void testReferenceHandlerPrimitiveReturn() throws Exception {
        // primitives should not allow nulls to return
        final String cStr = "{primInteger:{~ref=blah.ptr}}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final ReferenceHandler handler = mock(ReferenceHandler.class);
        when(handler.resolve(eq("blah.ptr"), eq("ptr"), same(config.getConfig("primInteger")), eq(int.class)))
                .thenReturn(null);
        util.addHandler("blah", handler);
        try {
            util.loadClass(new TestBean());
        } catch (RuntimeException e) {
            verify(handler, times(1)).resolve(eq("blah.ptr"), eq("ptr"), same(config.getConfig("primInteger")),
                    eq(int.class));
            assertThat(e).isExactlyInstanceOf(RuntimeException.class)
                    .hasMessageStartingWith("Primitive type int cannot resolve to null from handler for reference " +
                            "key blah.ptr sent to 'blah' handler with type");
        }

    }

    @Test
    public void testMapWireWithMap() throws Exception {
        // this method is limited to keys that are strings
        final String cStr = "" +
                "map:{" +
                "  blah:{" +
                "    ~type:com.github.flaminc.config.test.SimpleBean," +
                "    value:blah1" +
                "  }" +
                "}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final BeanWithMap beanWithMap = util.loadClass(new BeanWithMap());

        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setValue("blah1");
        assertThat(beanWithMap.getMap()).contains(entry("blah", simpleBean));
    }

    @Test
    public void testMapWireWithArray() throws Exception {
        // added this method as it would be the only way to add a non string key
        final String cStr = "" +
                "map:[" +
                "  blah,{" +
                "    ~type:com.github.flaminc.config.test.SimpleBean," +
                "    value:blah1" +
                "  }" +
                "]";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final BeanWithMap beanWithMap = util.loadClass(new BeanWithMap());

        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setValue("blah1");
        assertThat(beanWithMap.getMap()).contains(entry("blah", simpleBean));
    }

    @Test
    public void testEnum() throws Exception {
        final String cStr = "" +
                "value:foo";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final EnumBean enumBean = util.loadClass(new EnumBean());

        EnumBean expected = new EnumBean();
        expected.setValue(EnumInstance.foo);
        assertThat(enumBean).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testInheritanceBean() throws Exception {
        final String cStr = "" +
                "obj:{~type:com.github.flaminc.config.test.inheritance.Child}";
        final Config config = ConfigFactory.parseString(cStr).resolve();

        final ConfiguratorUtil util = ConfiguratorUtil.withConfig(config);
        final ParentBean enumBean = util.loadClass(new ParentBean());

        final Child child = new Child();
        assertThat(enumBean.getObj()).hasSameClassAs(child);
    }
}