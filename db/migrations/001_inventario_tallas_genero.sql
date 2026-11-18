-- Ejecutar SOLO en la BD LOCAL TiendaZapatosBD, en pgAdmin Query Tool.
-- La migración está pensada para la tabla inventario vacía (como en el respaldo).
-- Si inventario contiene filas, este script SE DETIENE sin modificarlas.
-- NO lo ejecutes en una base compartida ni lo repitas después de aplicarlo.

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.inventario LIMIT 1) THEN
        RAISE EXCEPTION 'La tabla inventario tiene registros. Se requiere una migracion de datos antes de cambiar zapato_id de INTEGER a UUID.';
    END IF;
END $$;

-- En el respaldo: inventario.zapato_id INTEGER y zapatos.zapato_id UUID.
-- Al estar inventario vacía, es seguro cambiar el tipo sin perder registros.
ALTER TABLE public.inventario
    ALTER COLUMN zapato_id TYPE UUID USING NULL::UUID,
    ALTER COLUMN zapato_id SET NOT NULL;

ALTER TABLE public.inventario
    ADD COLUMN genero VARCHAR(10) NOT NULL DEFAULT 'UNISEX',
    ADD COLUMN sistema_talla VARCHAR(5) NOT NULL DEFAULT 'US';

ALTER TABLE public.inventario
    ADD CONSTRAINT inventario_zapato_uuid_fkey
        FOREIGN KEY (zapato_id) REFERENCES public.zapatos(zapato_id),
    ADD CONSTRAINT inventario_genero_check
        CHECK (genero IN ('HOMBRE', 'MUJER', 'UNISEX')),
    ADD CONSTRAINT inventario_sistema_check
        CHECK (sistema_talla IN ('US', 'EU', 'UK'));

-- El índice único original no considera género ni sistema de talla.
ALTER TABLE public.inventario DROP CONSTRAINT IF EXISTS variante_unica;
ALTER TABLE public.inventario
    ADD CONSTRAINT variante_unica
    UNIQUE (zapato_id, genero, sistema_talla, talla, color);

COMMIT;
