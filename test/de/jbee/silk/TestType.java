package de.jbee.silk;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestType {

	@Test
	public void testToString() {
		Type<List> l = Type.rawType( List.class ).parametizedWith( String.class );
		assertThat( l.toString(), is( "java.util.List<java.lang.String>" ) );

		l = Type.rawType( List.class ).parametizedWith( Type.rawType( String.class ).asLoweBound() );
		assertThat( l.toString(), is( "java.util.List<? extends java.lang.String>" ) );
	}

	@Test
	public void shouldRecognize1DimensionalArrayTypes() {
		Type<Number[]> t = Type.rawType( Number[].class );
		assertTrue( t.is1DimensionArray() );
	}

	@Test
	public void shouldNotRecognizeMultiDimensionalArrayTypesAsArray1D() {
		Type<Number[][]> t = Type.rawType( Number[][].class );
		assertFalse( t.is1DimensionArray() );
	}

}