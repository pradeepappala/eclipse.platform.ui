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

package org.eclipse.core.tests.internal.databinding.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.eclipse.core.internal.databinding.conversion.StringToByteConverter;
import org.junit.Before;
import org.junit.Test;

import com.ibm.icu.text.NumberFormat;

/**
 * @since 1.1
 */
public class StringToByteConverterTest {
	private NumberFormat numberFormat;
	private StringToByteConverter converter;

	@Before
	public void setUp() throws Exception {
		numberFormat = NumberFormat.getIntegerInstance();
		converter = StringToByteConverter.toByte(numberFormat, false);
	}

	@Test
	public void testConvertsToByte() throws Exception {
		Byte value = Byte.valueOf((byte) 1);
		Byte result = (Byte) converter.convert(numberFormat.format(value));

		assertEquals(value, result);
	}

	@Test
	public void testConvertsToBytePrimitive() throws Exception {
		converter = StringToByteConverter.toByte(numberFormat, true);
		Byte value = Byte.valueOf((byte) 1);
		Byte result = (Byte) converter.convert(numberFormat.format(value));
		assertEquals(value, result);
	}

	@Test
	public void testFromTypeIsString() throws Exception {
		assertEquals(String.class, converter.getFromType());
	}

	@Test
	public void testToTypeIsShort() throws Exception {
		assertEquals(Byte.class, converter.getToType());
	}

	@Test
	public void testToTypeIsBytePrimitive() throws Exception {
		converter = StringToByteConverter.toByte(true);
		assertEquals(Byte.TYPE, converter.getToType());
	}

	@Test
	public void testReturnsNullBoxedTypeForEmptyString() throws Exception {
		assertNull(converter.convert(""));
	}

	@Test
	public void testThrowsIllegalArgumentExceptionIfAskedToConvertNonString()
			throws Exception {
		try {
			converter.convert(Integer.valueOf(1));
			fail("exception should have been thrown");
		} catch (IllegalArgumentException e) {
		}
	}
}
