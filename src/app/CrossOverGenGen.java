/*
 * CrossOverGenGen.java
 *
 * Created on 11 de enero de 2007, 10:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package app;

import org.jgap.*;
import org.jgap.impl.*;
import org.jgap.event.*;
import java.util.*;



/**
 *Selecciona do cromosonas al asar e intercambia los genes desde un punto de cruce aleatorio
 *en adelante teniendo en cuanta que el crude no se hace con los alelos del gen.
 **/
/**
 * The crossover operator randomly selects two Chromosomes from the
 * population and "mates" them by randomly picking a gene and then
 * swapping that gene and all subsequent genes between the two
 * Chromosomes. The two modified Chromosomes are then added to the
 * list of candidate Chromosomes.
 *
 * This CrossoverOperator supports both fixed and dynamic crossover rates.
 * A fixed rate is one specified at construction time by the user. This
 * operation is performed 1/m_crossoverRate as many times as there are
 * Chromosomes in the population. A dynamic rate is one determined by
 * this class if no fixed rate is provided.
 *
 * @author Neil Rotstan
 * @author Klaus Meffert
 * @author Chris Knowles
 * @since 1.0
 */
public class CrossOverGenGen
    extends BaseGeneticOperator implements Comparable {
  /** String containing the CVS revision. Read out via reflection!*/
  private final static String CVS_REVISION = "$Revision: 1.32 $";

  /**
   * The current crossover rate used by this crossover operator.
   */
  private int m_crossoverRate;

  /**
   * Calculator for dynamically determining the crossover rate. If set to
   * null the value of m_crossoverRate will be used instead.
   */
  private IUniversalRateCalculator m_crossoverRateCalc;

  /**
   * Constructs a new instance of this CrossoverOperator without a specified
   * crossover rate, this results in dynamic crossover rate being turned off.
   * This means that the crossover rate will be fixed at populationsize/2.<p>
   * Attention: The configuration used is the one set with the static method
   * Genotype.setConfiguration.
   * @throws InvalidConfigurationException
   *
   * @author Chris Knowles
   * @since 2.0
   */
  public CrossOverGenGen()
      throws InvalidConfigurationException {
    super(Genotype.getStaticConfiguration());
    //set the default crossoverRate to be populationsize/2
    m_crossoverRate = 2;
    setCrossoverRateCalc(null);
  }

  /**
   * Constructs a new instance of this CrossoverOperator without a specified
   * crossover rate, this results in dynamic crossover rate being turned off.
   * This means that the crossover rate will be fixed at populationsize/2.
   *
   * @param a_configuration the configuration to use
   * @throws InvalidConfigurationException
   *
   * @author Klaus Meffert
   * @since 3.0
   */
  public CrossOverGenGen(final Configuration a_configuration)
      throws InvalidConfigurationException {
    super(a_configuration);
    // Set the default crossoverRate to be populationsize/2.
    // -----------------------------------------------------
    m_crossoverRate = 2;
    setCrossoverRateCalc(null);
  }

  /**
   * Constructs a new instance of this CrossoverOperator with a specified
   * crossover rate calculator, which results in dynamic crossover being turned
   * on.
   *
   * @param a_configuration the configuration to use
   * @param a_crossoverRateCalculator calculator for dynamic crossover rate
   * computation
   * @throws InvalidConfigurationException
   *
   * @author Chris Knowles
   * @author Klaus Meffert
   * @since 3.0 (since 2.0 without a_configuration)
   */
  public CrossOverGenGen(final Configuration a_configuration,
                           final IUniversalRateCalculator
                           a_crossoverRateCalculator)
      throws InvalidConfigurationException {
    super(a_configuration);
    setCrossoverRateCalc(a_crossoverRateCalculator);
  }

  /**
   * Constructs a new instance of this CrossoverOperator with the given
   * crossover rate.
   *
   * @param a_configuration the configuration to use
   * @param a_desiredCrossoverRate the desired rate of crossover
   * @throws InvalidConfigurationException
   *
   * @author Chris Knowles
   * @author Klaus Meffert
   * @since 3.0 (since 2.0 without a_configuration)
   */
  public CrossOverGenGen(final Configuration a_configuration,
                           final int a_desiredCrossoverRate)
      throws InvalidConfigurationException {
    super(a_configuration);
    if (a_desiredCrossoverRate < 1) {
      throw new IllegalArgumentException("Crossover rate must be greater zero");
    }
    m_crossoverRate = a_desiredCrossoverRate;
    setCrossoverRateCalc(null);
  }

  /**
   * @author Neil Rotstan
   * @author Klaus Meffert
   * @since 2.0 (earlier versions referenced the Configuration object)
   */
  public void operate(final Population a_population,
                      final List a_candidateChromosomes) {
    // Work out the number of crossovers that should be performed.
    // -----------------------------------------------------------
    int size = Math.min(getConfiguration().getPopulationSize(),
                        a_population.size());
    int numCrossovers = 0;
    if (m_crossoverRateCalc == null) {
      numCrossovers = size / m_crossoverRate;
    }
    else {
      numCrossovers = size / m_crossoverRateCalc.calculateCurrentRate();
    }
    RandomGenerator generator = getConfiguration().getRandomGenerator();
    IGeneticOperatorConstraint constraint = getConfiguration().
        getJGAPFactory().getGeneticOperatorConstraint();
    // For each crossover, grab two random chromosomes, pick a random
    // locus (gene location), and then swap that gene and all genes
    // to the "right" (those with greater loci) of that gene between
    // the two chromosomes.
    // --------------------------------------------------------------
    int index1, index2;
    for (int i = 0; i < numCrossovers; i++) {
      index1 = generator.nextInt(size);
      index2 = generator.nextInt(size);
      IChromosome chrom1 = a_population.getChromosome(index1);
      IChromosome chrom2 = a_population.getChromosome(index2);
      // Verify that crossover allowed.
      // ------------------------------
      if (constraint != null) {
        List v = new Vector();
        v.add(chrom1);
        v.add(chrom2);
        if (!constraint.isValid(a_population, v, this)) {
          continue;
        }
      }
      // Clone the chromosomes.
      // ----------------------
      IChromosome firstMate = (IChromosome) chrom1.clone();
      IChromosome secondMate = (IChromosome) chrom2.clone();
      doCrossover(firstMate, secondMate, a_candidateChromosomes, generator);
    }
  }

  private void doCrossover(IChromosome firstMate, IChromosome secondMate,
                           List a_candidateChromosomes,
                           RandomGenerator generator) {
    Gene[] firstGenes = firstMate.getGenes();
    Gene[] secondGenes = secondMate.getGenes();
    int locus = generator.nextInt(firstGenes.length);
    // Intercambia genes.
    // ---------------
    Gene gene1;
    Gene gene2;
    ICompositeGene Gen1;
    Object firstAllele;
    for (int j = locus; j < firstGenes.length; j++) {
        
      gene1 = firstGenes[j];

      gene2 = secondGenes[j];
        
      firstAllele = gene1.getAllele();
      gene1.setAllele(gene2.getAllele());
      gene2.setAllele(firstAllele);
    }
    // Add the modified chromosomes to the candidate pool so that
    // they'll be considered for natural selection during the next
    // phase of evolution.
    // -----------------------------------------------------------
    a_candidateChromosomes.add(firstMate);
    a_candidateChromosomes.add(secondMate);
  }

  /**
   * Sets the crossover rate calculator.
   *
   * @param a_crossoverRateCalculator the new calculator
   *
   * @author Chris Knowles
   * @since 2.0
   */
  private void setCrossoverRateCalc(final IUniversalRateCalculator
                                    a_crossoverRateCalculator) {
    m_crossoverRateCalc = a_crossoverRateCalculator;
  }

  /**
   * Compares the given GeneticOperator to this GeneticOperator.
   *
   * @param a_other the instance against which to compare this instance
   * @return a negative number if this instance is "less than" the given
   * instance, zero if they are equal to each other, and a positive number if
   * this is "greater than" the given instance
   *
   * @author Klaus Meffert
   * @since 2.6
   */
  public int compareTo(final Object a_other) {
    /**@todo consider Configuration*/
    if (a_other == null) {
      return 1;
    }
    CrossOverGenGen op = (CrossOverGenGen) a_other;
    if (m_crossoverRateCalc == null) {
      if (op.m_crossoverRateCalc != null) {
        return -1;
      }
    }
    else {
      if (op.m_crossoverRateCalc == null) {
        return 1;
      }
    }
    if (m_crossoverRate != op.m_crossoverRate) {
      if (m_crossoverRate > op.m_crossoverRate) {
        return 1;
      }
      else {
        return -1;
      }
    }
    // Everything is equal. Return zero.
    // ---------------------------------
    return 0;
  }
}
