package plugins.tinevez.anisotropicdiffusion;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;

import java.util.List;

import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.tinevez.imglib2icy.ImgLib2IcySplitSequenceAdapter;

public class PMAnisotropicDiffusionImgLib2< T extends RealType< T >> extends EzPlug implements EzStoppable
{

	private final EzVarInteger nThreads = new EzVarInteger( "Use N threads", Runtime.getRuntime().availableProcessors() / 2, 1, Runtime.getRuntime().availableProcessors(), 1 );

	private final EzVarEnum< DiffEnum > diffusionType = new EzVarEnum< DiffEnum >( "Diffusion type", DiffEnum.values() );

	private final EzVarDouble kappa = new EzVarDouble( "Kappa", 10, 1e-3, Double.MAX_VALUE, 1e-3 );

	private final EzVarDouble dt = new EzVarDouble( "�t", 0.15, 1e-2, 100, 1e-2 );

	private final EzVarInteger nIterations = new EzVarInteger( "N iterations", 5, 1, 100000, 1 );

	private final EzVarBoolean aggregateC = new EzVarBoolean( "Process along C", false );

	private final EzVarBoolean aggregateZ = new EzVarBoolean( "Process along Z", false );

	private final EzVarBoolean aggregateT = new EzVarBoolean( "Process along T", false );

	private boolean stopRequested;

	@Override
	protected void initialize()
	{
		addEzComponent( diffusionType );
		addEzComponent( kappa );
		addEzComponent( nIterations );
		addEzComponent( nThreads );

		final EzGroup dimensionalityGroup = new EzGroup( "Dimensions", aggregateC, aggregateZ, aggregateT );
		addEzComponent( dimensionalityGroup );
	}

	@Override
	public void stopExecution()
	{
		stopRequested = true;
	}

	@Override
	protected void execute()
	{
		stopRequested = false;

		// Get current active image.
		final Sequence sequence = getActiveSequence();
		if ( null == sequence )
		{
			MessageDialog.showDialog( "Please select an image first.", MessageDialog.INFORMATION_MESSAGE );
			return;
		}

		// Wrap to ImgLib2.
		final boolean splitC = !aggregateC.getValue( true );
		final boolean splitZ = !aggregateZ.getValue( true );
		final boolean splitT = !aggregateT.getValue( true );
		final List< Img< T >> imgs = ImgLib2IcySplitSequenceAdapter.wrap( sequence, splitC, splitZ, splitT );

		// Create algo.

		for ( final Img< T > img : imgs )
		{

			final PeronaMalikAnisotropicDiffusion< T > algo = new PeronaMalikAnisotropicDiffusion< T >( img, 0.15, 15 );

			// N threads.
			algo.setNumThreads( nThreads.getValue( true ) );

			// Delta t
			algo.setDeltaT( dt.getValue( true ).floatValue() );

			// Diffusion type.
			final DiffusionFunction function = diffusionType.getValue().get( kappa.getValue( true ) );
			algo.setDiffusionFunction( function );

			if ( !algo.checkInput() )
			{
				MessageDialog.showDialog( "Check input failed! With: " + algo.getErrorMessage(), MessageDialog.ERROR_MESSAGE );
				return;
			}

			final int niter = nIterations.getValue( true );
			for ( int i = 0; i < niter; i++ )
			{
				if ( stopRequested )
				{
					break;
				}
				algo.process();
				sequence.dataChanged();
				getUI().setProgressBarValue( ( i + 1d ) / niter * imgs.size() );
			}

		}

		new AnnounceFrame( "Anisotropic diffusion done.", 2 );
	}


	@Override
	public void clean()
	{}


	private enum DiffEnum
	{

		STRONG_EDGE_ENHANCER( "Strong edge enhancer" ),
		WIDE_REGION_ENHANCER( "Wide region enhancer" );

		private final String name;

		private DiffEnum( final String name )
		{
			this.name = name;
		}

		public DiffusionFunction get( final double kappa )
		{
			switch ( this )
			{
			case STRONG_EDGE_ENHANCER:
			{
				return new PeronaMalikAnisotropicDiffusion.StrongEdgeEnhancer( kappa );
			}

			case WIDE_REGION_ENHANCER:
			default:
			{
				return new PeronaMalikAnisotropicDiffusion.WideRegionEnhancer( kappa );
			}
			}
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
