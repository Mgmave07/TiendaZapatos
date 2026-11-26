-- =============================================================================
-- DATOS DE PRUEBA — VENTAS Y DETALLES
-- =============================================================================
-- PROPÓSITO : Insertar ventas y líneas de producto de PRUEBA para poder
--             demostrar el "Reporte de Ventas por Producto" con datos reales.
-- BASE      : TiendaZapatodDB (la misma que usa el programa)
-- CÓMO USARLO:
--   1. Abre pgAdmin 4 → conéctate a TiendaZapatodDB.
--   2. Abre el Query Tool (Ctrl+T o clic en "Query Tool").
--   3. Pega o carga este archivo.
--   4. Ejecuta (F5).  Verifica que diga "DO" sin errores.
--   5. Reinicia el programa y genera el reporte.
-- IMPORTANTE:
--   • Este script NO elimina datos existentes; solo agrega filas de prueba.
--   • Todas las ventas de prueba tienen numero_venta con prefijo "PRUEBA-"
--     para distinguirlas de ventas reales y poder borrarlas fácilmente.
--   • Para eliminar los datos de prueba ejecuta el bloque DELETE al final.
--   • Requisito: la tabla inventario debe tener al menos 2 registros con
--     cantidad_stock > 0.  Si está vacía agrega productos desde el programa.
-- =============================================================================

DO $$
DECLARE
    -- Variables para IDs dinámicos (se leen de la BD real)
    v_vendedor_id   UUID;
    v_admin_id      UUID;
    v_inv1          UUID;
    v_inv2          UUID;
    v_inv3          UUID;

    -- IDs fijos para las ventas de prueba (se pueden borrar por prefijo)
    v_venta1 UUID := gen_random_uuid();
    v_venta2 UUID := gen_random_uuid();
    v_venta3 UUID := gen_random_uuid();

    -- Precio de los artículos (tomado de inventario→zapatos)
    v_precio1 NUMERIC;
    v_precio2 NUMERIC;
    v_precio3 NUMERIC;
BEGIN

    -- =========================================================================
    -- 1. Obtener un usuario con rol "Vendedor" (o cualquier usuario si no hay)
    -- =========================================================================
    SELECT u.usuario_id INTO v_vendedor_id
    FROM   usuarios u
    JOIN   roles    r ON u.rol_id = r.rol_id
    WHERE  LOWER(r.nombre) = 'vendedor'
    LIMIT  1;

    IF v_vendedor_id IS NULL THEN
        SELECT usuario_id INTO v_vendedor_id FROM usuarios LIMIT 1;
    END IF;

    SELECT u.usuario_id INTO v_admin_id
    FROM   usuarios u
    JOIN   roles    r ON u.rol_id = r.rol_id
    WHERE  LOWER(r.nombre) = 'administrador'
    LIMIT  1;

    IF v_admin_id IS NULL THEN
        v_admin_id := v_vendedor_id;
    END IF;

    -- =========================================================================
    -- 2. Obtener inventarios disponibles y sus precios
    -- =========================================================================
    SELECT i.inventario_id, z.precio_final
    INTO   v_inv1, v_precio1
    FROM   inventario i
    JOIN   zapatos    z ON i.zapato_id = z.zapato_id
    WHERE  i.cantidad_stock > 0
    ORDER  BY i.inventario_id
    LIMIT  1;

    SELECT i.inventario_id, z.precio_final
    INTO   v_inv2, v_precio2
    FROM   inventario i
    JOIN   zapatos    z ON i.zapato_id = z.zapato_id
    WHERE  i.cantidad_stock > 0
      AND  i.inventario_id <> COALESCE(v_inv1, '00000000-0000-0000-0000-000000000000'::UUID)
    ORDER  BY i.inventario_id
    LIMIT  1;

    SELECT i.inventario_id, z.precio_final
    INTO   v_inv3, v_precio3
    FROM   inventario i
    JOIN   zapatos    z ON i.zapato_id = z.zapato_id
    WHERE  i.cantidad_stock > 0
      AND  i.inventario_id NOT IN (
               COALESCE(v_inv1, '00000000-0000-0000-0000-000000000000'::UUID),
               COALESCE(v_inv2, '00000000-0000-0000-0000-000000000000'::UUID)
           )
    ORDER  BY i.inventario_id
    LIMIT  1;

    -- Usar defaults si el inventario tiene menos de 3 variantes
    v_precio1 := COALESCE(v_precio1, 1200.00);
    v_precio2 := COALESCE(v_precio2, 950.00);
    v_precio3 := COALESCE(v_precio3, v_precio1);
    IF v_inv3 IS NULL THEN v_inv3 := v_inv1; END IF;
    IF v_inv2 IS NULL THEN v_inv2 := v_inv1; END IF;

    IF v_inv1 IS NULL THEN
        RAISE EXCEPTION
            'No hay registros en la tabla inventario con cantidad_stock > 0. '
            'Registra al menos un producto con stock desde el programa antes de '
            'ejecutar este script.';
    END IF;

    -- =========================================================================
    -- 3. Venta PRUEBA-001 — hace 3 días, 2 artículos distintos
    -- =========================================================================
    INSERT INTO ventas (
        venta_id, numero_venta, fecha_venta,
        subtotal, impuesto, total,
        estado, metodo_pago, vendedor_id,
        cliente_nombre, cliente_identificacion, cliente_telefono, notas
    ) VALUES (
        v_venta1,
        'PRUEBA-001',
        NOW() - INTERVAL '3 days',
        v_precio1 + v_precio2,
        (v_precio1 + v_precio2) * 0.15,
        (v_precio1 + v_precio2) * 1.15,
        'PAGADA', 'Efectivo', v_vendedor_id,
        'María García (PRUEBA)', '001-280390-0012A', '8800-0001',
        'Venta de prueba — generada por datos_prueba_ventas.sql'
    );

    INSERT INTO detalles_venta (detalle_id, venta_id, inventario_id, cantidad, precio_unitario, subtotal)
    VALUES
        (gen_random_uuid(), v_venta1, v_inv1, 1, v_precio1, v_precio1),
        (gen_random_uuid(), v_venta1, v_inv2, 1, v_precio2, v_precio2);

    -- =========================================================================
    -- 4. Venta PRUEBA-002 — hace 2 días, 1 artículo × 2 unidades
    -- =========================================================================
    INSERT INTO ventas (
        venta_id, numero_venta, fecha_venta,
        subtotal, impuesto, total,
        estado, metodo_pago, vendedor_id,
        cliente_nombre, notas
    ) VALUES (
        v_venta2,
        'PRUEBA-002',
        NOW() - INTERVAL '2 days',
        v_precio1 * 2,
        v_precio1 * 2 * 0.15,
        v_precio1 * 2 * 1.15,
        'PAGADA', 'Tarjeta', v_vendedor_id,
        'Consumidor Final',
        'Venta de prueba — generada por datos_prueba_ventas.sql'
    );

    INSERT INTO detalles_venta (detalle_id, venta_id, inventario_id, cantidad, precio_unitario, subtotal)
    VALUES
        (gen_random_uuid(), v_venta2, v_inv1, 2, v_precio1, v_precio1 * 2);

    -- =========================================================================
    -- 5. Venta PRUEBA-003 — hace 1 día, vendedor = admin, 3 artículos distintos
    -- =========================================================================
    INSERT INTO ventas (
        venta_id, numero_venta, fecha_venta,
        subtotal, impuesto, total,
        estado, metodo_pago, vendedor_id,
        cliente_nombre, cliente_telefono, notas
    ) VALUES (
        v_venta3,
        'PRUEBA-003',
        NOW() - INTERVAL '1 day',
        v_precio1 + v_precio2 + v_precio3,
        (v_precio1 + v_precio2 + v_precio3) * 0.15,
        (v_precio1 + v_precio2 + v_precio3) * 1.15,
        'PAGADA', 'Transferencia', v_admin_id,
        'Carlos López (PRUEBA)', '8800-0003',
        'Venta de prueba — generada por datos_prueba_ventas.sql'
    );

    INSERT INTO detalles_venta (detalle_id, venta_id, inventario_id, cantidad, precio_unitario, subtotal)
    VALUES
        (gen_random_uuid(), v_venta3, v_inv1, 1, v_precio1, v_precio1),
        (gen_random_uuid(), v_venta3, v_inv2, 1, v_precio2, v_precio2),
        (gen_random_uuid(), v_venta3, v_inv3, 1, v_precio3, v_precio3);

    RAISE NOTICE 'Datos de prueba insertados exitosamente:';
    RAISE NOTICE '  PRUEBA-001 : % (vendedor_id: %)', v_venta1, v_vendedor_id;
    RAISE NOTICE '  PRUEBA-002 : % (vendedor_id: %)', v_venta2, v_vendedor_id;
    RAISE NOTICE '  PRUEBA-003 : % (admin_id:    %)', v_venta3, v_admin_id;
    RAISE NOTICE '  Inventarios usados: inv1=%, inv2=%, inv3=%', v_inv1, v_inv2, v_inv3;

END $$;

-- =============================================================================
-- ELIMINAR DATOS DE PRUEBA (ejecutar solo este bloque cuando quieras limpiar)
-- =============================================================================
-- DELETE FROM detalles_venta
--  WHERE venta_id IN (SELECT venta_id FROM ventas WHERE numero_venta LIKE 'PRUEBA-%');
-- DELETE FROM ventas WHERE numero_venta LIKE 'PRUEBA-%';
