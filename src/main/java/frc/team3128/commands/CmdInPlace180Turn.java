package frc.team3128.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.PIDCommand;
import frc.team3128.Constants.DriveConstants;
import frc.team3128.subsystems.NAR_Drivetrain;

public class CmdInPlace180Turn extends PIDCommand {

    private NAR_Drivetrain drivetrain;

    public CmdInPlace180Turn(NAR_Drivetrain drivetrain) {

        super(
            new PIDController(DriveConstants.TURN_kP, DriveConstants.TURN_kI, DriveConstants.TURN_kD),
            drivetrain::getHeading,
            MathUtil.inputModulus(drivetrain.getHeading() + 180, -180, 180),
            output -> drivetrain.tankDrive(output + Math.copySign(DriveConstants.TURN_kF, output), -output - Math.copySign(DriveConstants.TURN_kF, output)),
            drivetrain
        );

        this.drivetrain = drivetrain;

        getController().enableContinuousInput(-180, 180);
        getController().setTolerance(DriveConstants.TURN_TOLERANCE, DriveConstants.TURN_RATE_TOLERANCE);
    }

    @Override
    public void initialize() {
        super.initialize();

        // Hack to make sure the robot turns 180 degrees from current heading and not 180 degrees from 0
        double setpoint = MathUtil.inputModulus(drivetrain.getHeading() + 180, -180, 180);
        m_setpoint = () ->  setpoint;
    }

    public boolean isFinished() {
        return getController().atSetpoint();
    }

}
