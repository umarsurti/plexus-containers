package org.codehaus.plexus.component.configurator.converters.composite;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

/**
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class ObjectWithFieldsConverter extends AbstractConfigurationConverter
{
    public boolean canConvert( Class type )
    {
        boolean retValue = true;

        if ( Dictionary.class.isAssignableFrom( type ) )
        {
            retValue = false;
        }

        else if ( Map.class.isAssignableFrom( type ) )
        {
            retValue = false;
        }
        else if ( Collection.class.isAssignableFrom( type ) )
        {
            retValue = false;
        }

        return retValue;
    }

    public Object fromConfiguration( ConverterLookup converterLookup,
                                     PlexusConfiguration configuration,
                                     Class type,
                                     ClassLoader classLoader,
                                     ComponentDescriptor componentDescriptor ) throws ComponentConfigurationException
    {
        // it is a "composite"
        Class implementation = getClassForImplementationHint( type,
                configuration,
                classLoader,
                componentDescriptor );

        Object retValue = instantiateObject( implementation, componentDescriptor );

        processConfiguration( converterLookup, retValue, classLoader, configuration, componentDescriptor );

        return retValue;

    }


    public void processConfiguration( ConverterLookup converterLookup,
                                      Object object,
                                      ClassLoader classLoader,
                                      PlexusConfiguration configuration,
                                      ComponentDescriptor componentDescriptor )
            throws ComponentConfigurationException
    {
        int items = configuration.getChildCount();

        for ( int i = 0; i < items; i++ )
        {
            PlexusConfiguration childConfiguration = configuration.getChild( i );

            String elementName = childConfiguration.getName();

            String fieldName = fromXML( elementName );

            Field field = getFieldByName( fieldName, object, componentDescriptor );

            Class fieldType = field.getType();

            ConfigurationConverter converter = converterLookup.lookupConverterForType( fieldType );

            if ( converter == null )
            {
                String msg = "Error occured while configuring component ["
                        + componentDescriptor.getHumanReadableKey()
                        + "] No converter is capable to convert configuration entry <"
                        + elementName
                        + ">"
                        + " to instance of class: '"
                        + field.getType()
                        + "' Field name: '"
                        + fieldName
                        + "', declaring class: ' "
                        + object.getClass().getName()
                        + "'";

                throw new ComponentConfigurationException( msg );
            }

            Object value = converter.fromConfiguration( converterLookup, childConfiguration, fieldType, classLoader, componentDescriptor );

            setFieldValue( field, object, value, componentDescriptor );
        }


    }


    private void setFieldValue( Field field, Object object, Object value, ComponentDescriptor componentDescriptor )
            throws ComponentConfigurationException
    {
        try
        {
            boolean wasAccessible = field.isAccessible();

            if ( !wasAccessible )
            {
                field.setAccessible( true );
            }

            field.set( object, value );

            if ( !wasAccessible )
            {
                field.setAccessible( false );
            }
        }
        catch ( IllegalAccessException e )
        {
            String msg = "Error configuring component: "
                    + componentDescriptor.getHumanReadableKey()
                    + ". Cannot access field: '"
                    + field.getName() +
                    " in class: '"
                    + object.getClass().getName()
                    + "'";

            throw new ComponentConfigurationException( msg );
        }
    }

    private Field getFieldByName( String fieldName, Object object, ComponentDescriptor componentDescriptor )
            throws ComponentConfigurationException
    {

        Field retValue = ReflectionUtils.getFieldByNameIncludingSuperclasses( fieldName, object.getClass() );

        if ( retValue == null )
        {
            String msg = "Error configuring component: "
                    + componentDescriptor.getHumanReadableKey()
                    + ". Class '"
                    + object.getClass().getName()
                    + "' does not contain a field named '"
                    + fieldName + "'";

            throw new ComponentConfigurationException( msg );
        }

        return retValue;
    }

}