/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.databinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.conversion.IdentityConverter;
import org.eclipse.core.databinding.conversion.ToStringConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.util.Policy;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ObjectToPrimitiveValidator;
import org.eclipse.core.databinding.validation.String2BytePrimitiveValidator;
import org.eclipse.core.databinding.validation.String2ByteValidator;
import org.eclipse.core.databinding.validation.String2DateValidator;
import org.eclipse.core.databinding.validation.String2DoublePrimitiveValidator;
import org.eclipse.core.databinding.validation.String2DoubleValidator;
import org.eclipse.core.databinding.validation.String2FloatPrimitiveValidator;
import org.eclipse.core.databinding.validation.String2FloatValidator;
import org.eclipse.core.databinding.validation.String2IntegerPrimitiveValidator;
import org.eclipse.core.databinding.validation.String2IntegerValidator;
import org.eclipse.core.databinding.validation.String2LongPrimitiveValidator;
import org.eclipse.core.databinding.validation.String2LongValidator;
import org.eclipse.core.databinding.validation.String2ShortPrimitiveValidator;
import org.eclipse.core.databinding.validation.String2ShortValidator;
import org.eclipse.core.internal.databinding.ClassLookupSupport;
import org.eclipse.core.internal.databinding.Pair;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @since 3.3
 * 
 */
public class UpdateValueStrategy {

	/**
	 * Policy constant denoting that the source observable's state should not be
	 * tracked and that the destination observable's value should never be
	 * updated.
	 */
	public static int POLICY_NEVER = notInlined(1);

	/**
	 * Policy constant denoting that the source observable's state should not be
	 * tracked, but that validation, conversion and updating the destination
	 * observable's value should be performed when explicitly requested.
	 */
	public static int POLICY_ON_REQUEST = notInlined(2);

	/**
	 * Policy constant denoting that the source observable's state should be
	 * tracked, including validating changes except for
	 * {@link #validateBeforeSet(Object)}, but that the destination
	 * observable's value should only be updated on request.
	 */
	public static int POLICY_CONVERT = notInlined(4);

	/**
	 * Policy constant denoting that the source observable's state should be
	 * tracked, and that validation, conversion and updating the destination
	 * observable's value should be performed automaticlly on every change of
	 * the source observable value.
	 */
	public static int POLICY_UPDATE = notInlined(8);

	/**
	 * Helper method allowing API evolution of the above constant values. The
	 * compiler will not inline constant values into client code if values are
	 * "computed" using this helper.
	 * 
	 * @param i
	 *            an integer
	 * @return the same integer
	 */
	private static int notInlined(int i) {
		return i;
	}

	private static final String BOOLEAN_TYPE = "java.lang.Boolean.TYPE"; //$NON-NLS-1$

	private static final String SHORT_TYPE = "java.lang.Short.TYPE"; //$NON-NLS-1$

	private static final String BYTE_TYPE = "java.lang.Byte.TYPE"; //$NON-NLS-1$

	private static final String DOUBLE_TYPE = "java.lang.Double.TYPE"; //$NON-NLS-1$

	private static final String FLOAT_TYPE = "java.lang.Float.TYPE"; //$NON-NLS-1$

	private static final String INTEGER_TYPE = "java.lang.Integer.TYPE"; //$NON-NLS-1$

	private static final String LONG_TYPE = "java.lang.Long.TYPE"; //$NON-NLS-1$

	protected IValidator afterGetValidator;
	protected IValidator afterConvertValidator;
	protected IValidator beforeSetValidator;
	protected IConverter converter;

	private Map converterMap;

	private int updatePolicy;

	private static ValidatorRegistry validatorRegistry = new ValidatorRegistry();

	protected boolean provideDefaults;

	/**
	 * Creates a new update value strategy for automatically updating the
	 * destination observable value whenever the source observable value
	 * changes. Default validators and a default converter will be provided. The
	 * defaults can be changed by calling one of the setter methods.
	 */
	public UpdateValueStrategy() {
		this(true, POLICY_UPDATE);
	}

	/**
	 * Creates a new update value strategy with a configurable update policy.
	 * Default validators and a default converter will be provided. The defaults
	 * can be changed by calling one of the setter methods.
	 * 
	 * @param updatePolicy
	 *            one of {@link #POLICY_NEVER}, {@link #POLICY_ON_REQUEST},
	 *            {@link #POLICY_CONVERT}, or {@link #POLICY_UPDATE}
	 */
	public UpdateValueStrategy(int updatePolicy) {
		this(true, updatePolicy);
	}

	/**
	 * Creates a new update value strategy with a configurable update policy.
	 * Default validators and a default converter will be provided if
	 * <code>provideDefaults</code> is <code>true</code>. The defaults can
	 * be changed by calling one of the setter methods.
	 * 
	 * @param provideDefaults
	 *            if <code>true</code>, default validators and a default
	 *            converter will be provided based on the observable value's
	 *            type.
	 * @param updatePolicy
	 *            one of {@link #POLICY_NEVER}, {@link #POLICY_ON_REQUEST},
	 *            {@link #POLICY_CONVERT}, or {@link #POLICY_UPDATE}
	 */
	public UpdateValueStrategy(boolean provideDefaults, int updatePolicy) {
		this.provideDefaults = provideDefaults;
		this.updatePolicy = updatePolicy;
	}

	private Class autoboxed(Class clazz) {
		if (clazz == Float.TYPE)
			return Float.class;
		else if (clazz == Double.TYPE)
			return Double.class;
		else if (clazz == Short.TYPE)
			return Short.class;
		else if (clazz == Integer.TYPE)
			return Integer.class;
		else if (clazz == Long.TYPE)
			return Long.class;
		else if (clazz == Byte.TYPE)
			return Byte.class;
		else if (clazz == Boolean.TYPE)
			return Boolean.class;
		return clazz;
	}

	private void checkAssignable(Object toType, Object fromType,
			String errorString) {
		Boolean assignableFromModelToModelConverter = isAssignableFromTo(
				fromType, toType);
		if (assignableFromModelToModelConverter != null
				&& !assignableFromModelToModelConverter.booleanValue()) {
			throw new BindingException(errorString
					+ " Expected: " + fromType + ", actual: " + toType); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/**
	 * @param value
	 * @return the converted value
	 */
	public Object convert(Object value) {
		return converter == null ? value : converter.convert(value);
	}

	/**
	 * Tries to create a converter that can convert from values of type
	 * fromType. Returns <code>null</code> if no converter could be created.
	 * Either toType or modelDescription can be <code>null</code>, but not
	 * both.
	 * 
	 * @param fromType
	 * @param toType
	 * @return an IConverter, or <code>null</code> if unsuccessful
	 */
	protected IConverter createConverter(Object fromType, Object toType) {
		if (!(fromType instanceof Class) || !(toType instanceof Class)) {
			return new DefaultConverter(fromType, toType);
		}
		Class toClass = (Class) toType;
		if (toClass.isPrimitive()) {
			toClass = autoboxed(toClass);
		}
		Class fromClass = (Class) fromType;
		if (fromClass.isPrimitive()) {
			fromClass = autoboxed(fromClass);
		}
		if (!((Class) toType).isPrimitive()
				&& toClass.isAssignableFrom(fromClass)) {
			return new IdentityConverter(fromClass, toClass);
		}
		if (((Class) fromType).isPrimitive() && ((Class) toType).isPrimitive()
				&& fromType.equals(toType)) {
			return new IdentityConverter((Class) fromType, (Class) toType);
		}
		Map converterMap = getConverterMap();
		Class[] supertypeHierarchyFlattened = ClassLookupSupport
				.getTypeHierarchyFlattened(fromClass);
		for (int i = 0; i < supertypeHierarchyFlattened.length; i++) {
			Class currentFromClass = supertypeHierarchyFlattened[i];
			if (currentFromClass == toType) {
				// converting to toType is just a widening
				return new IdentityConverter(fromClass, toClass);
			}
			Pair key = new Pair(getKeyForClass(fromType, currentFromClass),
					getKeyForClass(toType, toClass));
			Object converterOrClassname = converterMap.get(key);
			if (converterOrClassname instanceof IConverter) {
				return (IConverter) converterOrClassname;
			} else if (converterOrClassname instanceof String) {
				String classname = (String) converterOrClassname;
				Class converterClass;
				try {
					converterClass = Class.forName(classname);
					IConverter result = (IConverter) converterClass
							.newInstance();
					converterMap.put(key, result);
					return result;
				} catch (Exception e) {
					Policy
							.getLog()
							.log(
									new Status(
											IStatus.ERROR,
											Policy.JFACE_DATABINDING,
											0,
											"Error while instantiating default converter", e)); //$NON-NLS-1$
				}
			}
		}
		// Since we found no converter yet, try a "downcast" converter;
		// the IdentityConverter will automatically check the actual types at
		// runtime.
		if (fromClass.isAssignableFrom(toClass)) {
			return new IdentityConverter(fromClass, toClass);
		}
		return new BindSpec.DefaultConverter(fromType, toType);
	}

	/**
	 * Tries to create a validator that can validate values of type fromType.
	 * Returns <code>null</code> if no validator could be created. Either
	 * toType or modelDescription can be <code>null</code>, but not both.
	 * 
	 * @param fromType
	 * @param toType
	 * @return an IValidator, or <code>null</code> if unsuccessful
	 */
	protected IValidator createValidator(Object fromType, Object toType) {
		if (fromType == null || toType == null) {
			return new IValidator() {

				public IStatus validate(Object value) {
					return Status.OK_STATUS;
				}
			};
		}

		IValidator dataTypeValidator = findValidator(fromType, toType);
		if (dataTypeValidator == null) {
			throw new BindingException(
					"No IValidator is registered for conversions from " + fromType + " to " + toType); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return dataTypeValidator;
	}

	/**
	 * 
	 * @param source
	 * @param destination
	 */
	protected void fillDefaults(IObservableValue source,
			IObservableValue destination) {
		Object sourceType = source.getValueType();
		Object destinationType = destination.getValueType();
		if (provideDefaults && sourceType != null && destinationType != null) {
			if (afterGetValidator == null) {
				afterGetValidator = createValidator(sourceType, destinationType);
			}
			if (converter == null) {
				setConverter(createConverter(sourceType, destinationType));
			}
		}
		if (converter != null) {
			if (sourceType != null) {
				checkAssignable(converter.getFromType(), sourceType,
						"converter does not convert from type " + sourceType); //$NON-NLS-1$
			}
			if (destinationType != null) {
				checkAssignable(converter.getToType(), destinationType,
						"converter does not convert to type " + destinationType); //$NON-NLS-1$
			}
		}
	}

	private IValidator findValidator(Object fromType, Object toType) {
		// TODO string-based lookup of validator
		return validatorRegistry.get(fromType, toType);
	}

	private Map getConverterMap() {
		// using string-based lookup avoids loading of too many classes
		if (converterMap == null) {
			converterMap = new HashMap();
			converterMap
					.put(
							new Pair("java.util.Date", "java.lang.String"), "org.eclipse.core.databinding.conversion.ConvertDate2String"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.math.BigDecimal"), "org.eclipse.core.databinding.conversion.ConvertString2BigDecimal"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Boolean"), "org.eclipse.core.databinding.conversion.ConvertString2Boolean"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Byte"), "org.eclipse.core.databinding.conversion.ConvertString2Byte"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Character"), "org.eclipse.core.databinding.conversion.ConvertString2Character"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.util.Date"), "org.eclipse.core.databinding.conversion.ConvertString2Date"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Double"), "org.eclipse.core.databinding.conversion.ConvertString2Double"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Float"), "org.eclipse.core.databinding.conversion.ConvertString2Float"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Integer"), "org.eclipse.core.databinding.conversion.ConvertString2Integer"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Long"), "org.eclipse.core.databinding.conversion.ConvertString2Long"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.String", "java.lang.Short"), "org.eclipse.core.databinding.conversion.ConvertString2Short"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			converterMap
					.put(
							new Pair("java.lang.Object", "java.lang.String"), "org.eclipse.core.databinding.conversion.ToStringConverter"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

			// Integer.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", INTEGER_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2IntegerPrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(INTEGER_TYPE, "java.lang.Integer"), new IdentityConverter(Integer.TYPE, Integer.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(INTEGER_TYPE, "java.lang.String"), new ToStringConverter(Integer.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(INTEGER_TYPE, "java.lang.Object"), new IdentityConverter(Integer.TYPE, Object.class)); //$NON-NLS-1$

			// Byte.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", BYTE_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2BytePrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(BYTE_TYPE, "java.lang.Byte"), new IdentityConverter(Byte.TYPE, Byte.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(BYTE_TYPE, "java.lang.String"), new ToStringConverter(Byte.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(BYTE_TYPE, "java.lang.Object"), new IdentityConverter(Byte.TYPE, Object.class)); //$NON-NLS-1$

			// Double.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", DOUBLE_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2DoublePrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(DOUBLE_TYPE, "java.lang.Double"), new IdentityConverter(Double.TYPE, Double.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(DOUBLE_TYPE, "java.lang.String"), new ToStringConverter(Double.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(DOUBLE_TYPE, "java.lang.Object"), new IdentityConverter(Double.TYPE, Object.class)); //$NON-NLS-1$

			// Boolean.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", BOOLEAN_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2BooleanPrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(BOOLEAN_TYPE, "java.lang.Boolean"), new IdentityConverter(Boolean.TYPE, Boolean.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(BOOLEAN_TYPE, "java.lang.String"), new ToStringConverter(Boolean.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(BOOLEAN_TYPE, "java.lang.Object"), new IdentityConverter(Boolean.TYPE, Object.class)); //$NON-NLS-1$

			// Float.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", FLOAT_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2FloatPrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(FLOAT_TYPE, "java.lang.Float"), new IdentityConverter(Float.TYPE, Float.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(FLOAT_TYPE, "java.lang.String"), new ToStringConverter(Float.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(FLOAT_TYPE, "java.lang.Object"), new IdentityConverter(Float.TYPE, Object.class)); //$NON-NLS-1$		

			// Short.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", SHORT_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2ShortPrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(SHORT_TYPE, "java.lang.Short"), new IdentityConverter(Short.TYPE, Short.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(SHORT_TYPE, "java.lang.String"), new ToStringConverter(Short.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(SHORT_TYPE, "java.lang.Object"), new IdentityConverter(Short.TYPE, Object.class)); //$NON-NLS-1$		

			// Long.TYPE
			converterMap
					.put(
							new Pair("java.lang.String", LONG_TYPE), "org.eclipse.core.databinding.conversion.ConvertString2LongPrimitive"); //$NON-NLS-1$ //$NON-NLS-2$
			converterMap
					.put(
							new Pair(LONG_TYPE, "java.lang.Long"), new IdentityConverter(Long.TYPE, Long.class)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(LONG_TYPE, "java.lang.String"), new ToStringConverter(Long.TYPE)); //$NON-NLS-1$
			converterMap
					.put(
							new Pair(LONG_TYPE, "java.lang.Object"), new IdentityConverter(Long.TYPE, Object.class)); //$NON-NLS-1$		

		}

		return converterMap;
	}

	private String getKeyForClass(Object originalValue, Class filteredValue) {
		if (originalValue instanceof Class) {
			Class originalClass = (Class) originalValue;
			if (originalClass.equals(Integer.TYPE)) {
				return INTEGER_TYPE;
			} else if (originalClass.equals(Byte.TYPE)) {
				return BYTE_TYPE;
			} else if (originalClass.equals(Boolean.TYPE)) {
				return BOOLEAN_TYPE;
			} else if (originalClass.equals(Double.TYPE)) {
				return DOUBLE_TYPE;
			} else if (originalClass.equals(Float.TYPE)) {
				return FLOAT_TYPE;
			} else if (originalClass.equals(Long.TYPE)) {
				return LONG_TYPE;
			} else if (originalClass.equals(Short.TYPE)) {
				return SHORT_TYPE;
			}
		}
		return filteredValue.getName();
	}

	/**
	 * @return the update policy
	 */
	public int getUpdatePolicy() {
		return updatePolicy;
	}

	/**
	 * @param fromType
	 * @param toType
	 * @return whether fromType is assignable to toType
	 */
	protected Boolean isAssignableFromTo(Object fromType, Object toType) {
		if (fromType instanceof Class && toType instanceof Class) {
			Class toClass = (Class) toType;
			if (toClass.isPrimitive()) {
				toClass = autoboxed(toClass);
			}
			Class fromClass = (Class) fromType;
			if (fromClass.isPrimitive()) {
				fromClass = autoboxed(fromClass);
			}
			return toClass.isAssignableFrom(fromClass) ? Boolean.TRUE
					: Boolean.FALSE;
		}
		return null;
	}

	/**
	 * @param validator
	 * @return the receiver, to enable method call chaining
	 */
	public UpdateValueStrategy setAfterConvertValidator(IValidator validator) {
		this.afterConvertValidator = validator;
		return this;
	}

	/**
	 * @param validator
	 * @return the receiver, to enable method call chaining
	 */
	public UpdateValueStrategy setAfterGetValidator(IValidator validator) {
		this.afterGetValidator = validator;
		return this;
	}

	/**
	 * @param validator
	 * @return the receiver, to enable method call chaining
	 */
	public UpdateValueStrategy setBeforeSetValidator(IValidator validator) {
		this.beforeSetValidator = validator;
		return this;
	}

	/**
	 * @param converter
	 * @return the receiver, to enable method call chaining
	 */
	public UpdateValueStrategy setConverter(IConverter converter) {
		this.converter = converter;
		return this;
	}

	/**
	 * @param value
	 * @return an ok status
	 */
	public IStatus validateAfterConvert(Object value) {
		return afterConvertValidator == null ? Status.OK_STATUS
				: afterConvertValidator.validate(value);
	}

	/**
	 * @param value
	 * @return an ok status
	 */
	public IStatus validateAfterGet(Object value) {
		return afterGetValidator == null ? Status.OK_STATUS : afterGetValidator
				.validate(value);
	}

	/**
	 * @param value
	 * @return an ok status
	 */
	public IStatus validateBeforeSet(Object value) {
		return beforeSetValidator == null ? Status.OK_STATUS
				: beforeSetValidator.validate(value);
	}
	
	/**
	 * Sets the current value of the given observable to the given value.
	 * Clients may extend but must call the super implementation.
	 * 
	 * @param observableValue
	 * @param value
	 */
	protected void doSet(IObservableValue observableValue, Object value) {
		observableValue.setValue(value);
	}

	private static class ValidatorRegistry {

		private HashMap validators = new HashMap();

		/**
		 * Adds the system-provided validators to the current validator
		 * registry. This is done automatically for the validator registry
		 * singleton.
		 */
		private ValidatorRegistry() {
			// Standalone validators here...
			associate(String.class, Integer.TYPE,
					new String2IntegerPrimitiveValidator());
			associate(String.class, Byte.TYPE,
					new String2BytePrimitiveValidator());
			associate(String.class, Short.TYPE,
					new String2ShortPrimitiveValidator());
			associate(String.class, Long.TYPE,
					new String2LongPrimitiveValidator());
			associate(String.class, Float.TYPE,
					new String2FloatPrimitiveValidator());
			associate(String.class, Double.TYPE,
					new String2DoublePrimitiveValidator());

			associate(String.class, Integer.class,
					new String2IntegerValidator());
			associate(String.class, Byte.class, new String2ByteValidator());
			associate(String.class, Short.class, new String2ShortValidator());
			associate(String.class, Long.class, new String2LongValidator());
			associate(String.class, Float.class, new String2FloatValidator());
			associate(String.class, Double.class, new String2DoubleValidator());
			associate(String.class, Date.class, new String2DateValidator());

			associate(Integer.class, Integer.TYPE,
					new ObjectToPrimitiveValidator(Integer.TYPE));
			associate(Byte.class, Byte.TYPE, new ObjectToPrimitiveValidator(
					Byte.TYPE));
			associate(Short.class, Short.TYPE, new ObjectToPrimitiveValidator(
					Short.TYPE));
			associate(Long.class, Long.TYPE, new ObjectToPrimitiveValidator(
					Long.TYPE));
			associate(Float.class, Float.TYPE, new ObjectToPrimitiveValidator(
					Float.TYPE));
			associate(Double.class, Double.TYPE,
					new ObjectToPrimitiveValidator(Double.TYPE));
			associate(Boolean.class, Boolean.TYPE,
					new ObjectToPrimitiveValidator(Boolean.TYPE));

			associate(Object.class, Integer.TYPE,
					new ObjectToPrimitiveValidator(Integer.TYPE));
			associate(Object.class, Byte.TYPE, new ObjectToPrimitiveValidator(
					Byte.TYPE));
			associate(Object.class, Short.TYPE, new ObjectToPrimitiveValidator(
					Short.TYPE));
			associate(Object.class, Long.TYPE, new ObjectToPrimitiveValidator(
					Long.TYPE));
			associate(Object.class, Float.TYPE, new ObjectToPrimitiveValidator(
					Float.TYPE));
			associate(Object.class, Double.TYPE,
					new ObjectToPrimitiveValidator(Double.TYPE));
			associate(Object.class, Boolean.TYPE,
					new ObjectToPrimitiveValidator(Boolean.TYPE));
		}

		/**
		 * Associate a particular validator that can validate the conversion
		 * (fromClass, toClass)
		 * 
		 * @param fromClass
		 *            The Class to convert from
		 * @param toClass
		 *            The Class to convert to
		 * @param validator
		 *            The IValidator
		 */
		private void associate(Object fromClass, Object toClass,
				IValidator validator) {
			validators.put(new Pair(fromClass, toClass), validator);
		}

		/**
		 * Return an IValidator for a specific fromClass and toClass.
		 * 
		 * @param fromClass
		 *            The Class to convert from
		 * @param toClass
		 *            The Class to convert to
		 * @return An appropriate IValidator
		 */
		private IValidator get(Object fromClass, Object toClass) {
			IValidator result = (IValidator) validators.get(new Pair(fromClass,
					toClass));
			if (result != null)
				return result;
			if (fromClass != null && toClass != null && fromClass == toClass) {
				return new IValidator() {
					public IStatus validate(Object value) {
						return Status.OK_STATUS;
					}
				};
			}
			return new IValidator() {
				public IStatus validate(Object value) {
					return Status.OK_STATUS;
				}
			};
		}
	}

	/*
	 * Default converter implementation, does not perform any conversion.
	 */
	static class DefaultConverter implements IConverter {

		private final Object toType;

		private final Object fromType;

		/**
		 * @param fromType
		 * @param toType
		 */
		DefaultConverter(Object fromType, Object toType) {
			this.toType = toType;
			this.fromType = fromType;
		}

		public Object convert(Object fromObject) {
			return fromObject;
		}

		public Object getFromType() {
			return fromType;
		}

		public Object getToType() {
			return toType;
		}
	}

}
