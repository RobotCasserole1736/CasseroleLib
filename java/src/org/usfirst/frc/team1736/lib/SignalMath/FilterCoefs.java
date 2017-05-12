package org.usfirst.frc.team1736.lib.SignalMath;

/*
 *******************************************************************************************
 * Copyright (C) 2017 FRC Team 1736 Robot Casserole - www.robotcasserole.org
 *******************************************************************************************
 *
 * This software is released under the MIT Licence - see the license.txt
 *  file in the root of this repo.
 *
 * Non-legally-binding statement from Team 1736:
 *  Thank you for taking the time to read through our software! We hope you
 *   find it educational and informative! 
 *  Please feel free to snag our software for your own use in whatever project
 *   you have going on right now! We'd love to be able to help out! Shoot us 
 *   any questions you may have, all our contact info should be on our website
 *   (listed above).
 *  If you happen to end up using our software to make money, that is wonderful!
 *   Robot Casserole is always looking for more sponsors, so we'd be very appreciative
 *   if you would consider donating to our club to help further STEM education.
 */


/**
 * Pre-calculated Coefficients used for filter calculations. For all, sample rate presumed at 50Hz
 * (20 ms). <br>
 * <br>
 * Derived from <a href="http://t-filter.engineerjs.com/" target="_blank"> this online source</a>.
 * Peter Isza, you are a boss and I owe you a beverage!
 */
final public class FilterCoefs {

    /* nothing to construct */
    private FilterCoefs() {
    }

    /**
     * 57-tap filter. Phase Delay = 0.56 s
     * <table style="border: 1px solid black">
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Zone</td>
     * <td style="border: 1px solid black">Start</td>
     * <td style="border: 1px solid black">End</td>
     * <td style="border: 1px solid black">Nominal Gain</td>
     * <td style="border: 1px solid black">Ripple</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Passband</td>
     * <td style="border: 1px solid black">0 Hz</td>
     * <td style="border: 1px solid black">2 Hz</td>
     * <td style="border: 1px solid black">0 dB</td>
     * <td style="border: 1px solid black">0.75 dB</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Stopband</td>
     * <td style="border: 1px solid black">4 Hz</td>
     * <td style="border: 1px solid black">25 Hz</td>
     * <td style="border: 1px solid black">-60 dB</td>
     * <td style="border: 1px solid black">0.02 dB</td>
     * </tr>
     * </table>
     */
    public static final double lowpass2HzCoef[] = {0.001042102726487998, 0.0014558783570545702, 0.002255087905497181,
            0.003110038282843073, 0.0038796661047761642, 0.004381546856654833, 0.004412124602978505,
            0.0037765975656177345, 0.0023259668992915597, -0.000002914521971847481, -0.0031532731575920755,
            -0.006921024012039051, -0.010949918087887964, -0.01473923854558122, -0.017686091529825716,
            -0.019136950996707643, -0.018463833559774583, -0.015147811554801164, -0.008856255155662874,
            0.0004883342145988067, 0.012663862801910133, 0.02713837043090747, 0.04309556988991669, 0.059493494634762986,
            0.07515683240665241, 0.08889018888323545, 0.09959952697136941, 0.10640582786864336, 0.10874007898178507,
            0.10640582786864336, 0.09959952697136941, 0.08889018888323545, 0.07515683240665241, 0.059493494634762986,
            0.04309556988991669, 0.02713837043090747, 0.012663862801910133, 0.0004883342145988067,
            -0.008856255155662874, -0.015147811554801164, -0.018463833559774583, -0.019136950996707643,
            -0.017686091529825716, -0.01473923854558122, -0.010949918087887964, -0.006921024012039051,
            -0.0031532731575920755, -0.000002914521971847481, 0.0023259668992915597, 0.0037765975656177345,
            0.004412124602978505, 0.004381546856654833, 0.0038796661047761642, 0.003110038282843073,
            0.002255087905497181, 0.0014558783570545702, 0.001042102726487998};

    /**
     * 57-tap filter. Phase Delay = 0.56 s
     * <table style="border: 1px solid black">
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Zone</td>
     * <td style="border: 1px solid black">Start</td>
     * <td style="border: 1px solid black">End</td>
     * <td style="border: 1px solid black">Nominal Gain</td>
     * <td style="border: 1px solid black">Ripple</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Passband</td>
     * <td style="border: 1px solid black">0 Hz</td>
     * <td style="border: 1px solid black">5 Hz</td>
     * <td style="border: 1px solid black">0 dB</td>
     * <td style="border: 1px solid black">0.63 dB</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Stopband</td>
     * <td style="border: 1px solid black">7 Hz</td>
     * <td style="border: 1px solid black">25 Hz</td>
     * <td style="border: 1px solid black">-60 dB</td>
     * <td style="border: 1px solid black">1.57 dB</td>
     * </tr>
     * </table>
     */
    public static final double lowpass5HzCoef[] = {-0.000830285228730819, -0.0028475683614571606, -0.004695820458162701,
            -0.006635300894355388, -0.006811051487077206, -0.004812237841446619, -0.0005315940351259668,
            0.004541239233426161, 0.008191030768037379, 0.0081643624879628, 0.0036134062642104083,
            -0.004103529440127811, -0.011503092902114721, -0.014366580219494308, -0.00988671359065656,
            0.0013785355185014646, 0.014928945369024959, 0.02375419821612738, 0.021507496018026116,
            0.006130639544709867, -0.017842631667283032, -0.039751662038594614, -0.04635180087909803,
            -0.027280164232119545, 0.019803732665245568, 0.08679447971969804, 0.15683502165343144, 0.20979791132719885,
            0.22950997175627985, 0.20979791132719885, 0.15683502165343144, 0.08679447971969804, 0.019803732665245568,
            -0.027280164232119545, -0.04635180087909803, -0.039751662038594614, -0.017842631667283032,
            0.006130639544709867, 0.021507496018026116, 0.02375419821612738, 0.014928945369024959,
            0.0013785355185014646, -0.00988671359065656, -0.014366580219494308, -0.011503092902114721,
            -0.004103529440127811, 0.0036134062642104083, 0.0081643624879628, 0.008191030768037379,
            0.004541239233426161, -0.0005315940351259668, -0.004812237841446619, -0.006811051487077206,
            -0.006635300894355388, -0.004695820458162701, -0.0028475683614571606, -0.000830285228730819};

    /**
     * 53-tap filter. Phase Delay = 0.52
     * <table style="border: 1px solid black">
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Zone</td>
     * <td style="border: 1px solid black">Start</td>
     * <td style="border: 1px solid black">End</td>
     * <td style="border: 1px solid black">Nominal Gain</td>
     * <td style="border: 1px solid black">Ripple</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Passband</td>
     * <td style="border: 1px solid black">0 Hz</td>
     * <td style="border: 1px solid black">15 Hz</td>
     * <td style="border: 1px solid black">0 dB</td>
     * <td style="border: 1px solid black">0.71 dB</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Stopband</td>
     * <td style="border: 1px solid black">17 Hz</td>
     * <td style="border: 1px solid black">25 Hz</td>
     * <td style="border: 1px solid black">-60 dB</td>
     * <td style="border: 1px solid black">0.47 dB</td>
     * </tr>
     * </table>
     */
    public static final double lowpass15HzCoef[] = {-0.006809180305744914, -0.011140713186520435, 0.0018805238949864416,
            0.01115809551274928, -0.0007985359156403671, -0.0016883898083527873, 0.010809826523195206,
            0.000376470073811836, -0.006563540652162264, 0.011618104734138003, 0.003853836196136715,
            -0.01278702561005925, 0.010994654534055105, 0.010776763385069478, -0.019632816157579884,
            0.007106275243106163, 0.02223042491601861, -0.02628201853358226, -0.0028657844985427246,
            0.040803080210918954, -0.0318406568631576, -0.02641103395967913, 0.07803677796791425, -0.0355519467404121,
            -0.11418957766240564, 0.2925092912500322, 0.6298143150792946, 0.2925092912500322, -0.11418957766240564,
            -0.0355519467404121, 0.07803677796791425, -0.02641103395967913, -0.0318406568631576, 0.040803080210918954,
            -0.0028657844985427246, -0.02628201853358226, 0.02223042491601861, 0.007106275243106163,
            -0.019632816157579884, 0.010776763385069478, 0.010994654534055105, -0.01278702561005925,
            0.003853836196136715, 0.011618104734138003, -0.006563540652162264, 0.000376470073811836,
            0.010809826523195206, -0.0016883898083527873, -0.0007985359156403671, 0.01115809551274928,
            0.0018805238949864416, -0.011140713186520435, -0.006809180305744914};


    /**
     * 81-tap filter. Phase Delay = 0.8s
     * <table style="border: 1px solid black">
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Zone</td>
     * <td style="border: 1px solid black">Start</td>
     * <td style="border: 1px solid black">End</td>
     * <td style="border: 1px solid black">Nominal Gain</td>
     * <td style="border: 1px solid black">Ripple</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Stopband</td>
     * <td style="border: 1px solid black">0 Hz</td>
     * <td style="border: 1px solid black">1.9 Hz</td>
     * <td style="border: 1px solid black">-50 dB</td>
     * <td style="border: 1px solid black">0.62 dB</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Passband</td>
     * <td style="border: 1px solid black">3 Hz</td>
     * <td style="border: 1px solid black">25 Hz</td>
     * <td style="border: 1px solid black">0 dB</td>
     * <td style="border: 1px solid black">0.70 dB</td>
     * </tr>
     * </table>
     */
    public static final double highpass2HzCoef[] = {-0.01497167039824472, -0.004865398824535514, 0.00896591884777147,
            -0.0011962679070584652, 0.007936710008511884, 0.0017571604482145057, 0.007409920932860388,
            0.0030974386803820585, 0.0058867874015511796, 0.002109941450378754, 0.0025989933665071887,
            -0.0011333104705008282, -0.0021001361305388734, -0.00549464585659025, -0.006748957412863571,
            -0.008992532643681295, -0.009267303795258973, -0.009498481045217746, -0.007870497768547834,
            -0.005703021580041328, -0.002041734191692866, 0.0020378475326501607, 0.006837912371559545,
            0.01137323055361255, 0.015570907651645776, 0.018534044773718027, 0.019995726323923064, 0.019277491784980925,
            0.016241542443959117, 0.010587449876894108, 0.0024072616900212836, -0.008130749208464, -0.02054557088325177,
            -0.0343177785075909, -0.04864137640028979, -0.06273240530680403, -0.07574209236654329, -0.0868432351382284,
            -0.09534170278857823, -0.10067500332867439, 0.8975103696532196, -0.10067500332867439, -0.09534170278857823,
            -0.0868432351382284, -0.07574209236654329, -0.06273240530680403, -0.04864137640028979, -0.0343177785075909,
            -0.02054557088325177, -0.008130749208464, 0.0024072616900212836, 0.010587449876894108, 0.016241542443959117,
            0.019277491784980925, 0.019995726323923064, 0.018534044773718027, 0.015570907651645776, 0.01137323055361255,
            0.006837912371559545, 0.0020378475326501607, -0.002041734191692866, -0.005703021580041328,
            -0.007870497768547834, -0.009498481045217746, -0.009267303795258973, -0.008992532643681295,
            -0.006748957412863571, -0.00549464585659025, -0.0021001361305388734, -0.0011333104705008282,
            0.0025989933665071887, 0.002109941450378754, 0.0058867874015511796, 0.0030974386803820585,
            0.007409920932860388, 0.0017571604482145057, 0.007936710008511884, -0.0011962679070584652,
            0.00896591884777147, -0.004865398824535514, -0.01497167039824472};

    /**
     * 55-tap filter. Phase Delay = 1.1s
     * <table style="border: 1px solid black">
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Zone</td>
     * <td style="border: 1px solid black">Start</td>
     * <td style="border: 1px solid black">End</td>
     * <td style="border: 1px solid black">Nominal Gain</td>
     * <td style="border: 1px solid black">Ripple</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Stopband</td>
     * <td style="border: 1px solid black">0 Hz</td>
     * <td style="border: 1px solid black">3 Hz</td>
     * <td style="border: 1px solid black">-60 dB</td>
     * <td style="border: 1px solid black">0.99 dB</td>
     * </tr>
     * 
     * <tr style="border: 1px solid black">
     * <td style="border: 1px solid black">Passband</td>
     * <td style="border: 1px solid black">5 Hz</td>
     * <td style="border: 1px solid black">25 Hz</td>
     * <td style="border: 1px solid black">0 dB</td>
     * <td style="border: 1px solid black">0.67 dB</td>
     * </tr>
     * </table>
     */
    public static final double highpass5HzCoef[] = {0.011318609012951645, -0.012681527102987767, -0.007117555196887919,
            -0.0029806100538400312, 0.0007977974162187222, 0.004363911182665122, 0.0071633418813247356,
            0.008301227861706622, 0.007010623881300626, 0.0030818647274722115, -0.0028602018855811637,
            -0.009336024978007965, -0.014322205099871869, -0.01574899829433337, -0.012267683064287297,
            -0.003683052853767167, 0.00847382366426991, 0.021304188108463903, 0.030809166596434914,
            0.032884219423007226, 0.024400563637633398, 0.004070676126310727, -0.02696067141839974, -0.0650241575025033,
            -0.10451680339651258, -0.1390013606480352, -0.16250154369482728, 0.8291525244662648, -0.16250154369482728,
            -0.1390013606480352, -0.10451680339651258, -0.0650241575025033, -0.02696067141839974, 0.004070676126310727,
            0.024400563637633398, 0.032884219423007226, 0.030809166596434914, 0.021304188108463903, 0.00847382366426991,
            -0.003683052853767167, -0.012267683064287297, -0.01574899829433337, -0.014322205099871869,
            -0.009336024978007965, -0.0028602018855811637, 0.0030818647274722115, 0.007010623881300626,
            0.008301227861706622, 0.0071633418813247356, 0.004363911182665122, 0.0007977974162187222,
            -0.0029806100538400312, -0.007117555196887919, -0.012681527102987767, 0.011318609012951645};

}
