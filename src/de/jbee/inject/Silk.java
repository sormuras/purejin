package de.jbee.inject;

import static de.jbee.inject.PreciserThanComparator.comparePrecision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Silk {

	public static enum CoreModule {
		PROVIDER( BuildinModule.class ),
		LIST( BuildinModule.class ),
		SET( BuildinModule.class );

		private final Class<? extends Bundle> bundle;

		private CoreModule( Class<? extends Bundle> bundle ) {
			this.bundle = bundle;
		}

	}

	public static Injector injector( Class<? extends Bundle> root ) {
		return Injector.create( root, new BuildinBundleBinder() );
	}

	static class BuildinBootstrapper
			implements Bootstrapper {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<Class<? extends Bundle>, Set<Class<? extends Bundle>>>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<Class<? extends Bundle>, List<Module>>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<Class<? extends Bundle>>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<Class<? extends Bundle>>();

		@Override
		public void install( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			if ( !stack.isEmpty() ) {
				final Class<? extends Bundle> parent = stack.peek();
				Set<Class<? extends Bundle>> children = bundleChildren.get( parent );
				if ( children == null ) {
					children = new LinkedHashSet<Class<? extends Bundle>>();
					bundleChildren.put( parent, children );
				}
				children.add( bundle );
			}
			stack.push( bundle );
			TypeReflector.newInstance( bundle ).bootstrap( this );
			if ( stack.pop() != bundle ) {
				throw new IllegalStateException( bundle.getCanonicalName() );
			}
		}

		@Override
		public void install( Module module ) {
			Class<? extends Bundle> bundle = stack.peek();
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			List<Module> modules = bundleModules.get( bundle );
			if ( modules == null ) {
				modules = new ArrayList<Module>();
				bundleModules.put( bundle, modules );
			}
			modules.add( module );
		}

		@Override
		public void uninstall( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			for ( Set<Class<? extends Bundle>> c : bundleChildren.values() ) {
				c.remove( bundle );
			}
			bundleModules.remove( bundle ); // we are sure we don't need its modules
			uninstalled.add( bundle );
		}

		public List<Module> installed( Class<? extends Bundle> root ) {
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<Class<? extends Bundle>>();
			allInstalledIn( root, installed );
			return modulesOf( installed );
		}

		public List<Module> modulesOf( Set<Class<? extends Bundle>> bundles ) {
			List<Module> installed = new ArrayList<Module>( bundles.size() );
			for ( Class<? extends Bundle> b : bundles ) {
				List<Module> modules = bundleModules.get( b );
				if ( modules != null ) {
					installed.addAll( modules );
				}
			}
			return installed;
		}

		private void allInstalledIn( Class<? extends Bundle> bundle,
				Set<Class<? extends Bundle>> accu ) {
			accu.add( bundle );
			Set<Class<? extends Bundle>> children = bundleChildren.get( bundle );
			if ( children == null ) {
				return;
			}
			for ( Class<? extends Bundle> c : children ) {
				if ( !accu.contains( c ) ) {
					allInstalledIn( c, accu );
				}
			}
		}
	}

	static class BuildinBundleBinder
			implements BundleBinder {

		// Find the initial set of bindings
		// 0. create BindInstruction
		// 2. sort instructions
		// 3. remove duplicates (implicit will be sorted after explicit)
		// 4. detect ambiguous bindings (two explicit bindings that have same type and availability)

		// 1. Create Scope-Repositories
		//   a. sort scopes from most stable to most fragile
		// 	 b. init one repository for each scope
		// 	 c. apply snapshots wrapper to repository instances
		@Override
		public Binding<?>[] install( Class<? extends Bundle> root ) {
			return bind( cleanedUp( declarationsFrom( root ) ) );
		}

		private Binding<?>[] bind( BindDeclaration<?>[] declarations ) {
			Map<Scope, Repository> repositories = buildRepositories( declarations );
			Binding<?>[] bindings = new Binding<?>[declarations.length];
			for ( int i = 0; i < declarations.length; i++ ) {
				BindDeclaration<?> instruction = declarations[i];
				bindings[i] = instruction.toBinding( repositories.get( instruction.scope() ) );
			}
			return bindings;
		}

		private Map<Scope, Repository> buildRepositories( BindDeclaration<?>[] instructions ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
			for ( BindDeclaration<?> i : instructions ) {
				Repository repository = repositories.get( i.scope() );
				if ( repository == null ) {
					repositories.put( i.scope(), i.scope().init( instructions.length ) );
				}
			}
			return repositories;
		}

		private BindDeclaration<?>[] cleanedUp( BindDeclaration<?>[] instructions ) {
			Arrays.sort( instructions );

			return instructions;
		}

		private BindDeclaration<?>[] declarationsFrom( Class<? extends Bundle> root ) {
			BuildinBootstrapper binder = new BuildinBootstrapper();
			binder.install( root );
			SimpleBindings bindings = new SimpleBindings();
			for ( Module m : binder.installed( root ) ) {
				m.configure( bindings );
			}
			return bindings.declarations.toArray( new BindDeclaration<?>[0] );
		}
	}

	static class SimpleBindings
			implements Bindings {

		final List<BindDeclaration<?>> declarations = new LinkedList<BindDeclaration<?>>();

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			declarations.add( new BindDeclaration<T>( declarations.size(), resource, supplier,
					scope, source ) );
		}

	}

	static final class BindDeclaration<T>
			implements Comparable<BindDeclaration<?>> {

		private final int nr;
		private final Resource<T> resource;
		private final Supplier<? extends T> supplier;
		private final Scope scope;
		private final Source source;

		BindDeclaration( int nr, Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			super();
			this.nr = nr;
			this.resource = resource;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		Resource<T> resource() {
			return resource;
		}

		Supplier<? extends T> supplier() {
			return supplier;
		}

		Scope scope() {
			return scope;
		}

		Source source() {
			return source;
		}

		Binding<T> toBinding( Repository repository ) {
			return new Binding<T>( resource, supplier, repository, source );
		}

		@Override
		public int compareTo( BindDeclaration<?> other ) {
			int res = comparePrecision( resource.getType(), other.resource.getType() );
			if ( res != 0 ) {
				return res;
			}
			res = comparePrecision( resource.getName(), other.resource.getName() );
			if ( res != 0 ) {
				return res;
			}
			res = comparePrecision( source, other.source );
			if ( res != 0 ) {
				return res;
			}
			return Integer.valueOf( nr ).compareTo( other.nr );
		}

	}

}
