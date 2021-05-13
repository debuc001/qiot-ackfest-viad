package de.viada.Service;

import io.quarkus.scheduler.Scheduled;
import de.viada.DTO.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MainService {

    private static final Logger LOG = Logger.getLogger(MainService.class);

    @ConfigProperty(name = "qiot.teamname")
    String teamname;
    @ConfigProperty(name = "qiot.coordinates.longitude")
    double longitude;
    @ConfigProperty(name = "qiot.coordinates.latitude")
    double latitude;
    @ConfigProperty(name = "qiot.coordinates.adress")
    String stationAdress;

    @Inject
    private CoordinateService coordinateService;
    @Inject
    @RestClient
    private SensorService sensorService;
    @Inject
    @RestClient
    private DataHubClientService dataHubClientService;

    @Inject
    private TelemetryService telemetryService;


    private int teamId;


    /**
     * PostContruct-Method to retreive station Coordinates and automatically register at the DataHub.
     */
    @PostConstruct
    public void register() throws IllegalStateException{
        System.out.println("" +
                "________  .___     ___________         .__                   __      _____                __   \n" +
                "\\_____  \\ |   | ___\\__    ___/         |  |__ _____    ____ |  | ___/ ____\\____   _______/  |_ \n" +
                " /  / \\  \\|   |/  _ \\|    |     ______ |  |  \\\\__  \\ _/ ___\\|  |/ /\\   __\\/ __ \\ /  ___/\\   __\\\n" +
                "/   \\_/.  \\   (  <_> )    |    /_____/ |   Y  \\/ __ \\\\  \\___|    <  |  | \\  ___/ \\___ \\  |  |  \n" +
                "\\_____\\ \\_/___|\\____/|____|            |___|  (____  /\\___  >__|_ \\ |__|  \\___  >____  > |__|  \n" +
                "       \\__>                                 \\/     \\/     \\/     \\/           \\/     \\/        ");

        System.out.println(
                "___________                     ____   ____.__            .___       \n" +
                        "\\__    ___/___ _____    _____   \\   \\ /   /|__|____     __| _/____   \n" +
                        "  |    |_/ __ \\\\__  \\  /     \\   \\   Y   / |  \\__  \\   / __ |\\__  \\  \n" +
                        "  |    |\\  ___/ / __ \\|  Y Y  \\   \\     /  |  |/ __ \\_/ /_/ | / __ \\_\n" +
                        "  |____| \\___  >____  /__|_|  /    \\___/   |__(____  /\\____ |(____  /\n" +
                        "             \\/     \\/      \\/                     \\/      \\/     \\/");

        /**
         * Try to retreive station Coordinates from a external Service.
         * If the Service is not available or fails, fallback Coordinates are used.
         */
        CoordinatesDTO coordinates = null;
        try {
            coordinates = this.coordinateService.getCoordinates(this.stationAdress);
        } catch (Exception ex) {
            LOG.warn("CoordinateService failed. Use fallback Coordinates.");
            coordinates = new CoordinatesDTO();
            coordinates.setLatitude(latitude);
            coordinates.setLongitude(longitude);
        }

        /**
         * Try to retreive the hardware-serial and register at the DataHub.
         */
        try {
            Serial serial = this.sensorService.getSerial();
            this.teamId = this.dataHubClientService.register(serial.getStationID(), teamname, coordinates.getLongitude(), coordinates.getLatitude());
        } catch (Exception ex) {
            LOG.error("Could not register at the DataHub.");
            throw new IllegalStateException("Could not register at the DataHub.");
        }
    }

    /**
     * Unregister at the DataHub before Services gets destroyed.
     */
    @PreDestroy
    public void unregister() {
        /**
         * Trying to unregister at the DataHub.
         */
        try {
            this.dataHubClientService.unregister(this.teamId);
            LOG.info("Unregistered from DataHub.");
        } catch (Exception ex) {
            LOG.error("Could not Unregister from DataHub.");
        }
    }

    @Scheduled(every = "5s")
    public void mainMethod(){
        System.out.println("TEST");
        try {
            GasDTO gasDTO = this.sensorService.getGas();
            GasTelemetry gasTelementry = new GasTelemetry(this.teamId, gasDTO);
            // send telemetry as json
            System.out.println("Gas Telemetry sucessfully sent.");
        } catch (Exception ex){
            System.out.println("Gas Telemetry could not be send.");
        }
        try {
            ParticulatesDTO particulatesDTO = this.sensorService.getParticulates();
            ParticulatesTelemetry particulatesTelemetry = new ParticulatesTelemetry(this.teamId, particulatesDTO);
            // send telemetry as json
            System.out.println("Particulates Telemetry sucessfully sent.");
        } catch (Exception ex){
            System.out.println("Particulates Telemetry could not be send.");
        }
    }
}
